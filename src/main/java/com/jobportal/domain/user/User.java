package com.jobportal.domain.user;

import com.jobportal.domain.common.BaseEntity;
import com.jobportal.domain.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Login identity for all three roles.
 * company is null for CANDIDATE and platform ADMIN; required for RECRUITER.
 * This is the entity Spring Security authenticates against.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_user")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified = false;
}