package com.jobportal.dto.job;

import com.jobportal.domain.job.EmploymentType;
import com.jobportal.domain.job.ExperienceLevel;
import com.jobportal.domain.job.Job;
import com.jobportal.domain.job.JobSkill;
import com.jobportal.domain.job.JobStatus;
import com.jobportal.domain.job.WorkMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record JobResponse(
        UUID id,
        UUID companyId,
        String companyName,
        String recruiterName,
        String title,
        String description,
        String requirements,
        String responsibilities,
        EmploymentType employmentType,
        ExperienceLevel experienceLevel,
        WorkMode workMode,
        String location,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String currency,
        int vacancies,
        LocalDate applicationDeadline,
        JobStatus status,
        List<String> skills,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static JobResponse from(Job job) {
        List<String> skillNames = job.getSkills().stream().map(JobSkill::getSkillName).toList();

        return new JobResponse(
                job.getId(),
                job.getCompany().getId(),
                job.getCompany().getName(),
                job.getCreatedByRecruiter().getFullName(),
                job.getTitle(),
                job.getDescription(),
                job.getRequirements(),
                job.getResponsibilities(),
                job.getEmploymentType(),
                job.getExperienceLevel(),
                job.getWorkMode(),
                job.getLocation(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getCurrency(),
                job.getVacancies(),
                job.getApplicationDeadline(),
                job.getStatus(),
                skillNames,
                job.getPublishedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}