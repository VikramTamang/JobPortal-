package com.jobportal.repository;

import com.jobportal.domain.application.Application;
import com.jobportal.domain.application.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    /** Candidate-scoped access — same tenant-scoping principle as Phase 4/5, just keyed on candidateId instead of companyId. */
    Optional<Application> findByIdAndCandidateId(UUID id, UUID candidateId);

    /** Recruiter-scoped access. */
    Optional<Application> findByIdAndCompanyId(UUID id, UUID companyId);

    Page<Application> findByCandidateId(UUID candidateId, Pageable pageable);

    Page<Application> findByCompanyId(UUID companyId, Pageable pageable);

    Page<Application> findByCompanyIdAndStatus(UUID companyId, ApplicationStatus status, Pageable pageable);

    Page<Application> findByJobIdAndCompanyId(UUID jobId, UUID companyId, Pageable pageable);
}