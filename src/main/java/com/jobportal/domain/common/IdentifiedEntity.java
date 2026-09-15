package com.jobportal.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Gives every entity a Hibernate-generated UUID primary key.
 * Use this directly (not BaseEntity) for tables whose timestamp columns
 * don't follow the generic created_at/updated_at shape — e.g. Resume
 * (uploaded_at), ApplicationStatusHistory (changed_at), Notification
 * (created_at + sent_at only), AuditLog (created_at only).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class IdentifiedEntity {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;
}