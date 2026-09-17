package com.jobportal.repository;

import com.jobportal.domain.application.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {

    List<ApplicationStatusHistory> findByApplicationIdOrderByChangedAtAsc(UUID applicationId);

    /**
     * DISTINCT application_id, not COUNT(*) — an application can pass
     * through a status more than once in edge cases (unlikely with this
     * workflow's one-way transitions, but the history table doesn't
     * enforce that), and we want "how many applications ever reached
     * this stage," not "how many times this transition fired."
     */
    @Query(value = """
            SELECT COUNT(DISTINCT h.application_id) FROM application_status_history h
            JOIN application a ON a.id = h.application_id
            WHERE a.company_id = :companyId AND h.new_status = :status
            """, nativeQuery = true)
    long countDistinctApplicationsReachedStatus(@Param("companyId") UUID companyId, @Param("status") String status);
}