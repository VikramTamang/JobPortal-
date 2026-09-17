package com.jobportal.repository;

import com.jobportal.domain.application.Application;
import com.jobportal.domain.application.ApplicationStatus;
import com.jobportal.repository.projection.ApplicationStatusCountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    boolean existsByResumeId(UUID resumeId);
    boolean existsByResumeIdAndCompanyId(UUID resumeId, UUID companyId);

    long countByCompanyId(UUID companyId);

    /** Platform-wide, for Admin analytics — no company scoping. */
    long countByStatus(ApplicationStatus status);

    @Query("SELECT a.status as status, COUNT(a) as count FROM Application a WHERE a.company.id = :companyId GROUP BY a.status")
    List<ApplicationStatusCountView> countGroupedByStatusForCompany(@Param("companyId") UUID companyId);
}