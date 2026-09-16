package com.jobportal.service;

import com.jobportal.domain.application.Application;
import com.jobportal.domain.application.ApplicationStatus;
import com.jobportal.domain.application.ApplicationStatusHistory;
import com.jobportal.domain.job.Job;
import com.jobportal.domain.job.JobStatus;
import com.jobportal.domain.resume.Resume;
import com.jobportal.domain.user.CandidateProfile;
import com.jobportal.domain.user.User;
import com.jobportal.dto.application.ApplicationResponse;
import com.jobportal.dto.application.ApplicationStatusHistoryResponse;
import com.jobportal.dto.application.ApplyToJobRequest;
import com.jobportal.dto.application.UpdateApplicationStatusRequest;
import com.jobportal.exception.BadRequestException;
import com.jobportal.exception.DuplicateResourceException;
import com.jobportal.exception.InvalidApplicationStateException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.ApplicationStatusHistoryRepository;
import com.jobportal.repository.CandidateProfileRepository;
import com.jobportal.repository.JobRepository;
import com.jobportal.repository.ResumeRepository;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final JobRepository jobRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    /**
     * Recruiter-driven forward pipeline: APPLIED -> SCREENING -> SHORTLISTED
     * -> INTERVIEW -> OFFER -> HIRED. REJECTED is reachable from every
     * non-terminal state (a recruiter can reject at any stage). HIRED and
     * REJECTED are terminal — nothing moves out of them. WITHDRAWN is
     * candidate-initiated and handled separately in withdraw(), not through
     * this map, since a candidate withdrawing isn't a recruiter decision.
     */
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> RECRUITER_TRANSITIONS = new EnumMap<>(ApplicationStatus.class);
    static {
        RECRUITER_TRANSITIONS.put(ApplicationStatus.APPLIED, EnumSet.of(ApplicationStatus.SCREENING, ApplicationStatus.REJECTED));
        RECRUITER_TRANSITIONS.put(ApplicationStatus.SCREENING, EnumSet.of(ApplicationStatus.SHORTLISTED, ApplicationStatus.REJECTED));
        RECRUITER_TRANSITIONS.put(ApplicationStatus.SHORTLISTED, EnumSet.of(ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED));
        RECRUITER_TRANSITIONS.put(ApplicationStatus.INTERVIEW, EnumSet.of(ApplicationStatus.OFFER, ApplicationStatus.REJECTED));
        RECRUITER_TRANSITIONS.put(ApplicationStatus.OFFER, EnumSet.of(ApplicationStatus.HIRED, ApplicationStatus.REJECTED));
        RECRUITER_TRANSITIONS.put(ApplicationStatus.HIRED, EnumSet.noneOf(ApplicationStatus.class));
        RECRUITER_TRANSITIONS.put(ApplicationStatus.REJECTED, EnumSet.noneOf(ApplicationStatus.class));
        RECRUITER_TRANSITIONS.put(ApplicationStatus.WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class));
    }

    private static final Set<ApplicationStatus> WITHDRAWABLE_FROM = EnumSet.of(
            ApplicationStatus.APPLIED, ApplicationStatus.SCREENING, ApplicationStatus.SHORTLISTED,
            ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER);

    @Transactional
    public ApplicationResponse apply(ApplyToJobRequest request) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(currentUserService.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        Job job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new BadRequestException("This job is not currently accepting applications");
        }
        if (job.getApplicationDeadline() != null && job.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw new BadRequestException("The application deadline for this job has passed");
        }
        if (applicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())) {
            throw new DuplicateResourceException("You have already applied to this job");
        }

        Resume resume = null;
        if (request.resumeId() != null) {
            resume = resumeRepository.findByIdAndCandidateId(request.resumeId(), candidate.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
        }

        Application application = new Application();
        application.setJob(job);
        application.setCompany(job.getCompany());
        application.setCandidate(candidate);
        application.setResume(resume);
        application.setCoverLetter(request.coverLetter());
        application.setStatus(ApplicationStatus.APPLIED);
        applicationRepository.save(application);

        recordHistory(application, null, ApplicationStatus.APPLIED, currentUserEntity(), null);

        notificationService.enqueue(
                candidate.getUser(),
                "APPLICATION_SUBMITTED",
                "Application submitted: " + job.getTitle(),
                "Your application for \"" + job.getTitle() + "\" at " + job.getCompany().getName() + " has been received.",
                "APPLICATION", application.getId());

        return ApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getForCurrentUser(UUID applicationId) {
        return ApplicationResponse.from(loadForCurrentUser(applicationId));
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> listMyApplications(Pageable pageable) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(currentUserService.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));
        return applicationRepository.findByCandidateId(candidate.getId(), pageable).map(ApplicationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> listForCompany(ApplicationStatus statusFilter, Pageable pageable) {
        UUID companyId = currentUserService.getCompanyId();
        Page<Application> page = statusFilter != null
                ? applicationRepository.findByCompanyIdAndStatus(companyId, statusFilter, pageable)
                : applicationRepository.findByCompanyId(companyId, pageable);
        return page.map(ApplicationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> listForJob(UUID jobId, Pageable pageable) {
        UUID companyId = currentUserService.getCompanyId();

        // Confirms the job belongs to this recruiter's company BEFORE listing
        // -- otherwise a recruiter probing another tenant's job id would get
        // a 200 with an empty page instead of a clean 404, which still leaks
        // "this job id exists, it's just not yours".
        jobRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        return applicationRepository.findByJobIdAndCompanyId(jobId, companyId, pageable).map(ApplicationResponse::from);
    }

    @Transactional
    public ApplicationResponse updateStatus(UUID applicationId, UpdateApplicationStatusRequest request) {
        Application application = applicationRepository.findByIdAndCompanyId(applicationId, currentUserService.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        ApplicationStatus current = application.getStatus();
        ApplicationStatus target = request.status();

        Set<ApplicationStatus> allowed = RECRUITER_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidApplicationStateException(
                    "Cannot move application from " + current + " to " + target
                            + ". Allowed next states: " + (allowed.isEmpty() ? "none (terminal)" : allowed));
        }

        application.setStatus(target);
        applicationRepository.save(application);

        recordHistory(application, current, target, currentUserEntity(), request.note());

        notificationService.enqueue(
                application.getCandidate().getUser(),
                "APPLICATION_STATUS_CHANGED",
                "Your application status changed: " + application.getJob().getTitle(),
                "Your application for \"" + application.getJob().getTitle() + "\" is now " + target + ".",
                "APPLICATION", application.getId());

        return ApplicationResponse.from(application);
    }

    @Transactional
    public ApplicationResponse withdraw(UUID applicationId) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(currentUserService.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        Application application = applicationRepository.findByIdAndCandidateId(applicationId, candidate.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        ApplicationStatus current = application.getStatus();
        if (!WITHDRAWABLE_FROM.contains(current)) {
            throw new InvalidApplicationStateException("Cannot withdraw an application that is already " + current);
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.save(application);

        recordHistory(application, current, ApplicationStatus.WITHDRAWN, currentUserEntity(), null);

        return ApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationStatusHistoryResponse> getHistory(UUID applicationId) {
        Application application = loadForCurrentUser(applicationId);
        return historyRepository.findByApplicationIdOrderByChangedAtAsc(application.getId())
                .stream().map(ApplicationStatusHistoryResponse::from).toList();
    }

    /** Candidate sees their own application; recruiter sees any application in their company. Same 404-not-403 principle as everywhere else. */
    private Application loadForCurrentUser(UUID applicationId) {
        if (currentUserService.isRecruiter()) {
            return applicationRepository.findByIdAndCompanyId(applicationId, currentUserService.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        }
        CandidateProfile candidate = candidateProfileRepository.findByUserId(currentUserService.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));
        return applicationRepository.findByIdAndCandidateId(applicationId, candidate.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private User currentUserEntity() {
        return userRepository.findById(currentUserService.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void recordHistory(Application application, ApplicationStatus previous, ApplicationStatus next, User changedBy, String note) {
        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setPreviousStatus(previous);
        history.setNewStatus(next);
        history.setChangedBy(changedBy);
        history.setNote(note);
        historyRepository.save(history);
    }
}