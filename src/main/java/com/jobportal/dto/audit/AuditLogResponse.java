package com.jobportal.dto.audit;

import com.jobportal.domain.audit.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID companyId,
        String companyName,
        UUID actorId,
        String actorEmail,
        String action,
        String entityType,
        UUID entityId,
        String metadata,
        String ipAddress,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getCompany() != null ? log.getCompany().getId() : null,
                log.getCompany() != null ? log.getCompany().getName() : null,
                log.getActor() != null ? log.getActor().getId() : null,
                log.getActor() != null ? log.getActor().getEmail() : null,
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getMetadata(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}