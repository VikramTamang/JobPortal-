package com.jobportal.repository;

import com.jobportal.domain.job.Job;
import com.jobportal.domain.job.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdAndCompanyId(UUID id, UUID companyId);

    Page<Job> findByCompanyId(UUID companyId, Pageable pageable);

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    /**
     * Every filter is optional and expressed as (:param IS NULL OR ...), so
     * a single query handles every combination the candidate might send —
     * no dynamic SQL string-building, no injection surface. Sort order is
     * chosen with CASE WHEN :sortMode = N rather than relying on Pageable's
     * generic Sort, because Pageable.getSort() maps to JPA entity property
     * names, not raw SQL column names, and this is a native query.
     *
     * Uses the FULLTEXT index on (title, description) for keyword search —
     * the whole reason that index exists in the schema.
     */
    @Query(
            value = """
            SELECT j.* FROM job j
            WHERE j.status = 'PUBLISHED'
              AND (:keyword IS NULL OR MATCH(j.title, j.description) AGAINST (:keyword IN NATURAL LANGUAGE MODE))
              AND (:location IS NULL OR j.location LIKE CONCAT('%', :location, '%'))
              AND (:employmentType IS NULL OR j.employment_type = :employmentType)
              AND (:experienceLevel IS NULL OR j.experience_level = :experienceLevel)
              AND (:workMode IS NULL OR j.work_mode = :workMode)
              AND (:salaryMin IS NULL OR j.salary_max IS NULL OR j.salary_max >= :salaryMin)
              AND (:salaryMax IS NULL OR j.salary_min IS NULL OR j.salary_min <= :salaryMax)
              AND (:postedAfter IS NULL OR j.published_at >= :postedAfter)
              AND (:deadlineBefore IS NULL OR j.application_deadline <= :deadlineBefore)
              AND (:hasSkillFilter = FALSE OR EXISTS (
                    SELECT 1 FROM job_skill js WHERE js.job_id = j.id AND js.skill_name IN (:skills)
                  ))
            ORDER BY
              CASE WHEN :sortMode = 0 AND :keyword IS NOT NULL
                   THEN MATCH(j.title, j.description) AGAINST (:keyword IN NATURAL LANGUAGE MODE) END DESC,
              CASE WHEN :sortMode = 1 THEN j.published_at END DESC,
              CASE WHEN :sortMode = 2 THEN j.published_at END ASC,
              CASE WHEN :sortMode = 3 THEN j.salary_max END DESC,
              CASE WHEN :sortMode = 4 THEN j.salary_min END ASC,
              CASE WHEN :sortMode = 5 THEN j.application_deadline END ASC,
              j.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM job j
            WHERE j.status = 'PUBLISHED'
              AND (:keyword IS NULL OR MATCH(j.title, j.description) AGAINST (:keyword IN NATURAL LANGUAGE MODE))
              AND (:location IS NULL OR j.location LIKE CONCAT('%', :location, '%'))
              AND (:employmentType IS NULL OR j.employment_type = :employmentType)
              AND (:experienceLevel IS NULL OR j.experience_level = :experienceLevel)
              AND (:workMode IS NULL OR j.work_mode = :workMode)
              AND (:salaryMin IS NULL OR j.salary_max IS NULL OR j.salary_max >= :salaryMin)
              AND (:salaryMax IS NULL OR j.salary_min IS NULL OR j.salary_min <= :salaryMax)
              AND (:postedAfter IS NULL OR j.published_at >= :postedAfter)
              AND (:deadlineBefore IS NULL OR j.application_deadline <= :deadlineBefore)
              AND (:hasSkillFilter = FALSE OR EXISTS (
                    SELECT 1 FROM job_skill js WHERE js.job_id = j.id AND js.skill_name IN (:skills)
                  ))
            """,
            nativeQuery = true
    )
    Page<Job> search(
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("employmentType") String employmentType,
            @Param("experienceLevel") String experienceLevel,
            @Param("workMode") String workMode,
            @Param("salaryMin") BigDecimal salaryMin,
            @Param("salaryMax") BigDecimal salaryMax,
            @Param("postedAfter") LocalDate postedAfter,
            @Param("deadlineBefore") LocalDate deadlineBefore,
            @Param("hasSkillFilter") boolean hasSkillFilter,
            @Param("skills") List<String> skills,
            @Param("sortMode") int sortMode,
            Pageable pageable
    );
}