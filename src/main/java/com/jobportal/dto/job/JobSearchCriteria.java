package com.jobportal.dto.job;

import com.jobportal.domain.job.EmploymentType;
import com.jobportal.domain.job.ExperienceLevel;
import com.jobportal.domain.job.WorkMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record JobSearchCriteria(
        String keyword,
        String location,
        EmploymentType employmentType,
        ExperienceLevel experienceLevel,
        WorkMode workMode,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        List<String> skills,
        LocalDate postedAfter,
        LocalDate deadlineBefore,
        JobSortOption sortBy
) {
}