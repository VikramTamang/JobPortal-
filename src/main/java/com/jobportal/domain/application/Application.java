package com.jobportal.domain.application;

import com.jobportal.domain.common.IdentifiedEntity;
import com.jobportal.domain.company.Company;
import com.jobportal.domain.job.Job;
import com.jobportal.domain.resume.Resume;
import com.jobportal.domain.user.CandidateProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "application",
        uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "candidate_id"}))
@EntityListeners(AuditingEntityListener.class)
public class Application extends IdentifiedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    /**
     * Denormalized from job.company — copied at creation time so every
     * recruiter-facing query can filter directly on this column without a
     * join, which is both a performance win and a tenant-isolation
     * guarantee (see Phase 4 service layer).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfile candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @CreatedDate
    @Column(name = "applied_at", nullable = false, updatable = false)
    private Instant appliedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}