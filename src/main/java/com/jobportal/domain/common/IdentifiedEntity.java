package com.jobportal.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Gives every entity a Hibernate-generated UUID primary key, stored as
 * CHAR(36). MySQL has no native UUID type — without
 * @JdbcTypeCode(SqlTypes.CHAR), Hibernate would default to BINARY(16),
 * which doesn't match the migration SQL and isn't human-readable for
 * debugging. This annotation lives on the base class so it applies to
 * every entity's id AND to the matching foreign-key columns Hibernate
 * generates for @ManyToOne relationships.
 *
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
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(updatable = false, nullable = false, length = 36)
    private UUID id;
}