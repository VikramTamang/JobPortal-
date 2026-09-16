package com.jobportal.dto.job;

import com.jobportal.domain.job.EmploymentType;
import com.jobportal.domain.job.ExperienceLevel;
import com.jobportal.domain.job.WorkMode;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Shared by both create and update — a job posting has the same shape either way. */
public record JobRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String description,
        String requirements,
        String responsibilities,
        @NotNull EmploymentType employmentType,
        @NotNull ExperienceLevel experienceLevel,
        @NotNull WorkMode workMode,
        @Size(max = 255) String location,
        @PositiveOrZero BigDecimal salaryMin,
        @PositiveOrZero BigDecimal salaryMax,
        @Size(max = 10) String currency,
        @Positive Integer vacancies,
        @FutureOrPresent LocalDate applicationDeadline,
        List<@NotBlank String> skills
) {
}