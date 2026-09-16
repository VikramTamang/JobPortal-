package com.jobportal.dto.recruiter;

import java.util.UUID;

public record RecruiterProfileResponse(
        UUID id,
        String fullName,
        String jobTitle,
        String department,
        String email,
        UUID companyId,
        String companyName
) {
}