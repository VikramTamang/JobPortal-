package com.jobportal.service;

import com.jobportal.domain.resume.Resume;
import com.jobportal.domain.user.CandidateProfile;
import com.jobportal.dto.resume.ResumeDownloadResult;
import com.jobportal.dto.resume.ResumeResponse;
import com.jobportal.exception.BadRequestException;
import com.jobportal.exception.FileTooLargeException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.exception.TenantAccessDeniedException;
import com.jobportal.exception.UnsupportedFileTypeException;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.CandidateProfileRepository;
import com.jobportal.repository.ResumeRepository;
import com.jobportal.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "application/pdf", ".pdf",
            "application/msword", ".doc",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"
    );

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB per resume

    private final ResumeRepository resumeRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final ApplicationRepository applicationRepository;
    private final com.jobportal.storage.FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @Transactional
    public ResumeResponse upload(MultipartFile file, boolean requestedPrimary) {
        validate(file);

        CandidateProfile candidate = candidateProfileRepository.findByUserId(currentUserService.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        String extension = ALLOWED_TYPES.get(file.getContentType());
        String storageKey = "resumes/" + candidate.getId() + "/" + UUID.randomUUID() + extension;

        try {
            fileStorageService.store(file, storageKey);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store uploaded file", e);
        }

        boolean isFirstResume = resumeRepository.countByCandidateId(candidate.getId()) == 0;
        boolean makePrimary = requestedPrimary || isFirstResume;

        if (makePrimary) {
            clearExistingPrimary(candidate.getId());
        }

        Resume resume = new Resume();
        resume.setCandidate(candidate);
        resume.setFileName(sanitizeDisplayName(file.getOriginalFilename()));
        resume.setStorageKey(storageKey);
        resume.setContentType(file.getContentType());
        resume.setFileSizeBytes(file.getSize());
        resume.setPrimary(makePrimary);

        resumeRepository.save(resume);
        return ResumeResponse.from(resume);
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> listMine() {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(currentUserService.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));
        return resumeRepository.findByCandidateIdOrderByUploadedAtDesc(candidate.getId())
                .stream().map(ResumeResponse::from).toList();
    }

    @Transactional
    public ResumeResponse setPrimary(UUID resumeId) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(currentUserService.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        Resume resume = resumeRepository.findByIdAndCandidateId(resumeId, candidate.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        clearExistingPrimary(candidate.getId());
        resume.setPrimary(true);
        resumeRepository.save(resume);
        return ResumeResponse.from(resume);
    }

    @Transactional
    public void delete(UUID resumeId) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(currentUserService.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        Resume resume = resumeRepository.findByIdAndCandidateId(resumeId, candidate.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        if (applicationRepository.existsByResumeId(resume.getId())) {
            throw new BadRequestException(
                    "This resume has already been submitted with an application and can't be deleted. "
                            + "Withdraw the application first, or upload a different resume for future applications.");
        }

        fileStorageService.delete(resume.getStorageKey());
        resumeRepository.delete(resume);
    }

    /**
     * The access-control rule from the spec: a candidate can always access
     * their own resume; a recruiter can access it ONLY if it's attached to
     * an application submitted to their company. Both "resume doesn't
     * exist" and "resume exists but you have no right to see it" return the
     * same 404 — see ApplicationRepository.existsByResumeIdAndCompanyId.
     */
    @Transactional(readOnly = true)
    public ResumeDownloadResult download(UUID resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        boolean authorized;
        if (currentUserService.isCandidate()) {
            authorized = resume.getCandidate().getUser().getId().equals(currentUserService.getUserId());
        } else if (currentUserService.isRecruiter()) {
            authorized = applicationRepository.existsByResumeIdAndCompanyId(resumeId, currentUserService.getCompanyId());
        } else {
            authorized = false; // admins don't get a blanket resume-download bypass
        }

        if (!authorized) {
            throw new ResourceNotFoundException("Resume not found");
        }

        byte[] content;
        try {
            content = fileStorageService.load(resume.getStorageKey());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read stored file", e);
        }

        if (currentUserService.isRecruiter()) {
            auditService.record("RESUME_ACCESSED", "RESUME", resume.getId());
        }

        return new ResumeDownloadResult(content, resume.getFileName(), resume.getContentType());
    }

    private void clearExistingPrimary(UUID candidateId) {
        resumeRepository.findByCandidateIdAndPrimaryTrue(candidateId)
                .forEach(r -> { r.setPrimary(false); resumeRepository.save(r); });
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileTooLargeException("Resume file exceeds the 5MB size limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.containsKey(contentType)) {
            throw new UnsupportedFileTypeException(
                    "Unsupported file type. Allowed types: PDF, DOC, DOCX. Received: " + contentType);
        }
    }

    /** Strip any path components from the client-supplied filename before storing it for display. */
    private String sanitizeDisplayName(String originalFilename) {
        if (originalFilename == null) return "resume";
        String name = originalFilename.replace("\\", "/");
        int lastSlash = name.lastIndexOf('/');
        return lastSlash >= 0 ? name.substring(lastSlash + 1) : name;
    }
}