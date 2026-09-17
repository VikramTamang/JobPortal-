package com.jobportal.dto.analytics;

import com.jobportal.domain.application.ApplicationStatus;
import com.jobportal.domain.job.JobStatus;

import java.util.Map;

public record PlatformAnalyticsResponse(
        long totalCompanies,
        long activeCompanies,
        long totalCandidates,
        long totalRecruiters,
        long totalJobs,
        long totalApplications,
        Map<JobStatus, Long> jobsByStatus,
        Map<ApplicationStatus, Long> applicationsByStatus
) {
}