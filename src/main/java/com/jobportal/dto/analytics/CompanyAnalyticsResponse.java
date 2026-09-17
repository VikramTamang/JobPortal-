package com.jobportal.dto.analytics;

import com.jobportal.domain.application.ApplicationStatus;

import java.util.List;
import java.util.Map;

public record CompanyAnalyticsResponse(
        long totalJobs,
        long activeJobs,
        long totalApplications,
        double avgApplicationsPerJob,
        Map<ApplicationStatus, Long> applicationsByStatus,
        List<JobApplicationCount> applicationsPerJob,
        double shortlistingRate,
        double interviewConversionRate,
        double offerConversionRate,
        double hiringConversionRate
) {
}