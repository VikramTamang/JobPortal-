package com.jobportal.dto.application;

import com.jobportal.domain.application.ApplicationStatus;
import com.jobportal.domain.application.ApplicationStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record ApplicationStatusHistoryResponse(
        UUID id,
        ApplicationStatus previousStatus,
        ApplicationStatus newStatus,
        String changedByEmail,
        String note,
        Instant changedAt
) {
    public static ApplicationStatusHistoryResponse from(ApplicationStatusHistory h) {
        return new ApplicationStatusHistoryResponse(
                h.getId(),
                h.getPreviousStatus(),
                h.getNewStatus(),
                h.getChangedBy().getEmail(),
                h.getNote(),
                h.getChangedAt()
        );
    }
}