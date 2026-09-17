package com.jobportal.repository;

import com.jobportal.domain.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /** Every filter optional, same pattern as JobRepository.search from Phase 6. */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:companyId IS NULL OR a.company.id = :companyId)
              AND (:actorId IS NULL OR a.actor.id = :actorId)
              AND (:action IS NULL OR a.action = :action)
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:fromDate IS NULL OR a.createdAt >= :fromDate)
              AND (:toDate IS NULL OR a.createdAt <= :toDate)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> search(
            @Param("companyId") UUID companyId,
            @Param("actorId") UUID actorId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );
}