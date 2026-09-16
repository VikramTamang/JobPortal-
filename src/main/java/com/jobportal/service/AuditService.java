package com.jobportal.service;

import com.jobportal.domain.audit.AuditLog;
import com.jobportal.domain.user.User;
import com.jobportal.repository.AuditLogRepository;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * A minimal writer for now — just enough to record who accessed what.
 * Phase 11 builds the full audit trail (login, job publish, admin actions,
 * etc.) plus a query/reporting endpoint. Introduced here specifically
 * because the spec calls out "resume access" by name as something that
 * must be tracked, and this is the phase that introduces resume access.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @Transactional
    public void record(String action, String entityType, UUID entityId) {
        User actor = userRepository.findById(currentUserService.getUserId()).orElse(null);

        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        if (actor != null && actor.getCompany() != null) {
            log.setCompany(actor.getCompany());
        }
        auditLogRepository.save(log);
    }
}