package com.jobportal.service;

import com.jobportal.domain.user.RecruiterProfile;
import com.jobportal.dto.recruiter.RecruiterProfileResponse;
import com.jobportal.dto.recruiter.UpdateRecruiterProfileRequest;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.RecruiterProfileRepository;
import com.jobportal.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecruiterProfileService {

    private final RecruiterProfileRepository recruiterProfileRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public RecruiterProfileResponse getMyProfile() {
        return toResponse(loadMyProfile());
    }

    @Transactional
    public RecruiterProfileResponse updateMyProfile(UpdateRecruiterProfileRequest request) {
        RecruiterProfile profile = loadMyProfile();
        profile.setFullName(request.fullName());
        profile.setJobTitle(request.jobTitle());
        profile.setDepartment(request.department());
        recruiterProfileRepository.save(profile);
        return toResponse(profile);
    }

    private RecruiterProfile loadMyProfile() {
        // Scoped by the authenticated user's own id — again, nothing here
        // for a request to override.
        return recruiterProfileRepository.findByUserId(currentUserService.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));
    }

    private RecruiterProfileResponse toResponse(RecruiterProfile p) {
        return new RecruiterProfileResponse(
                p.getId(), p.getFullName(), p.getJobTitle(), p.getDepartment(),
                p.getUser().getEmail(), p.getCompany().getId(), p.getCompany().getName());
    }
}