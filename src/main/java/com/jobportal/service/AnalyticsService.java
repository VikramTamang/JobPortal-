package com.jobportal.service;

import com.jobportal.domain.application.ApplicationStatus;
import com.jobportal.domain.job.JobStatus;
import com.jobportal.dto.analytics.CompanyAnalyticsResponse;
import com.jobportal.dto.analytics.JobApplicationCount;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.ApplicationStatusHistoryRepository;
import com.jobportal.repository.JobRepository;
import com.jobportal.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public CompanyAnalyticsResponse getCompanyAnalytics() {
        UUID companyId = currentUserService.getCompanyId();

        long totalJobs = jobRepository.countByCompanyId(companyId);
        long activeJobs = jobRepository.countByCompanyIdAndStatus(companyId, JobStatus.PUBLISHED);
        long totalApplications = applicationRepository.countByCompanyId(companyId);

        Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) {
            byStatus.put(s, 0L); // stable shape: every status present even at zero
        }
        applicationRepository.countGroupedByStatusForCompany(companyId)
                .forEach(v -> byStatus.put(v.getStatus(), v.getCount()));

        List<JobApplicationCount> perJob = jobRepository.applicationCountsByJobForCompany(companyId);

        double avgPerJob = totalJobs == 0 ? 0.0 : (double) totalApplications / totalJobs;

        long reachedShortlisted = historyRepository.countDistinctApplicationsReachedStatus(companyId, "SHORTLISTED");
        long reachedInterview = historyRepository.countDistinctApplicationsReachedStatus(companyId, "INTERVIEW");
        long reachedOffer = historyRepository.countDistinctApplicationsReachedStatus(companyId, "OFFER");
        long reachedHired = historyRepository.countDistinctApplicationsReachedStatus(companyId, "HIRED");

        return new CompanyAnalyticsResponse(
                totalJobs,
                activeJobs,
                totalApplications,
                avgPerJob,
                byStatus,
                perJob,
                rate(reachedShortlisted, totalApplications),
                rate(reachedInterview, totalApplications),
                rate(reachedOffer, totalApplications),
                rate(reachedHired, totalApplications)
        );
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }
}