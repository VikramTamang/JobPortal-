package com.jobportal.domain.audit;

import com.jobportal.domain.common.IdentifiedEntity;
import com.jobportal.domain.company.Company;
import com.jobportal.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Platform-wide activity trail. company is nullable — platform-level admin
 * actions (e.g. deactivating a company) have no tenant of their own.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_log")
@EntityListeners(AuditingEntityListener.class)
public class AuditLog extends IdentifiedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    /** e.g. JOB_PUBLISHED, APPLICATION_STATUS_CHANGED, RESUME_ACCESSED */
    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}