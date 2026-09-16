package com.jobportal.dto.recruiter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRecruiterProfileRequest(
        @NotBlank @Size(max = 255) String fullName,
        String jobTitle,
        String department
) {
}