package com.jobportal.repository;

import com.jobportal.domain.job.Job;
import com.jobportal.domain.job.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    /** The tenant-scoped lookup every mutation (update/publish/close/archive/delete) must use. */
    Optional<Job> findByIdAndCompanyId(UUID id, UUID companyId);

    /** Recruiter's own "my jobs" list — every status, own company only. */
    Page<Job> findByCompanyId(UUID companyId, Pageable pageable);

    /** Public listing — published jobs only, visible to anyone. Superseded by real search in Phase 6. */
    Page<Job> findByStatus(JobStatus status, Pageable pageable);
}