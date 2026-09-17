package com.jobportal.service;

import com.jobportal.domain.audit.AuditLog;
import com.jobportal.domain.user.User;
import com.jobportal.dto.audit.AuditLogResponse;
import com.jobportal.repository.AuditLogRepository;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /** For actions performed by the currently authenticated user — the common case (job actions, resume access, etc). */
    @Transactional
    public void record(String action, String entityType, UUID entityId) {
        record(action, entityType, entityId, null);
    }

    @Transactional
    public void record(String action, String entityType, UUID entityId, Map<String, Object> metadata) {
        User actor = userRepository.findById(currentUserService.getUserId()).orElse(null);
        write(actor, action, entityType, entityId, metadata);
    }

    /**
     * For actions with no authenticated SecurityContext — login and
     * registration happen on permitAll endpoints, before any JWT exists,
     * so CurrentUserService has nothing to read from. The caller already
     * has the User in hand (just loaded or just created), so it's passed
     * in directly instead.
     */
    @Transactional
    public void recordForActor(User actor, String action, String entityType, UUID entityId) {
        write(actor, action, entityType, entityId, null);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(UUID companyId, UUID actorId, String action, String entityType,
                                         Instant fromDate, Instant toDate, Pageable pageable) {
        return auditLogRepository.search(companyId, actorId, action, entityType, fromDate, toDate, pageable)
                .map(AuditLogResponse::from);
    }

    private void write(User actor, String action, String entityType, UUID entityId, Map<String, Object> metadata) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        if (actor != null && actor.getCompany() != null) {
            log.setCompany(actor.getCompany());
        }
        if (metadata != null && !metadata.isEmpty()) {
            try {
                log.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (Exception e) {
                log.warn("Failed to serialize audit metadata for action {}: {}", action, e.getMessage());
            }
        }
        auditLogRepository.save(log);
    }
}