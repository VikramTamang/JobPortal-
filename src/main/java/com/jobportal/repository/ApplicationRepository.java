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

    Optional<Application> findByIdAndCandidateId(UUID id, UUID candidateId);
    Optional<Application> findByIdAndCompanyId(UUID id, UUID companyId);

    Page<Application> findByCandidateId(UUID candidateId, Pageable pageable);
    Page<Application> findByCompanyId(UUID companyId, Pageable pageable);
    Page<Application> findByCompanyIdAndStatus(UUID companyId, ApplicationStatus status, Pageable pageable);
    Page<Application> findByJobIdAndCompanyId(UUID jobId, UUID companyId, Pageable pageable);

    /** Resume-deletion guard: don't let a candidate delete a resume they've already submitted somewhere. */
    boolean existsByResumeId(UUID resumeId);

    /**
     * THE resume access-control query: is this resume attached to any
     * application belonging to this recruiter's company? If yes, the
     * candidate applied to one of their jobs and the recruiter may view the
     * resume. If no — whether the resume doesn't exist, or exists but was
     * never sent to this company — the recruiter gets the same 404 either
     * way, per the same not-found-vs-forbidden principle used everywhere else.
     */
    boolean existsByResumeIdAndCompanyId(UUID resumeId, UUID companyId);
}