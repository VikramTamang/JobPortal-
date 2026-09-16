package com.jobportal.dto.application;

import com.jobportal.domain.application.Application;
import com.jobportal.domain.application.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID jobId,
        String jobTitle,
        UUID companyId,
        String companyName,
        UUID candidateId,
        String candidateName,
        UUID resumeId,
        String resumeFileName,
        String coverLetter,
        ApplicationStatus status,
        Instant appliedAt,
        Instant updatedAt
) {
    public static ApplicationResponse from(Application a) {
        return new ApplicationResponse(
                a.getId(),
                a.getJob().getId(),
                a.getJob().getTitle(),
                a.getCompany().getId(),
                a.getCompany().getName(),
                a.getCandidate().getId(),
                a.getCandidate().getFullName(),
                a.getResume() != null ? a.getResume().getId() : null,
                a.getResume() != null ? a.getResume().getFileName() : null,
                a.getCoverLetter(),
                a.getStatus(),
                a.getAppliedAt(),
                a.getUpdatedAt()
        );
    }
}