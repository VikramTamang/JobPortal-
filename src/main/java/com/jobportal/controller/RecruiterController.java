package com.jobportal.controller;

import com.jobportal.dto.recruiter.RecruiterProfileResponse;
import com.jobportal.dto.recruiter.UpdateRecruiterProfileRequest;
import com.jobportal.service.RecruiterProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recruiters")
@RequiredArgsConstructor
@Tag(name = "Recruiters", description = "Recruiter self-service for their own profile")
public class RecruiterController {

    private final RecruiterProfileService recruiterProfileService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Get the current recruiter's own profile")
    public RecruiterProfileResponse getMyProfile() {
        return recruiterProfileService.getMyProfile();
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Update the current recruiter's own profile")
    public RecruiterProfileResponse updateMyProfile(@Valid @RequestBody UpdateRecruiterProfileRequest request) {
        return recruiterProfileService.updateMyProfile(request);
    }
}