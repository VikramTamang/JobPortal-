package com.jobportal.service;

import com.jobportal.domain.application.ApplicationStatus;
import com.jobportal.domain.job.JobStatus;
import com.jobportal.domain.user.Role;
import com.jobportal.dto.analytics.PlatformAnalyticsResponse;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.CompanyRepository;
import com.jobportal.repository.JobRepository;
import com.jobportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public PlatformAnalyticsResponse getPlatformAnalytics() {
        Map<JobStatus, Long> jobsByStatus = new EnumMap<>(JobStatus.class);
        for (JobStatus s : JobStatus.values()) {
            jobsByStatus.put(s, jobRepository.countByStatus(s));
        }

        Map<ApplicationStatus, Long> applicationsByStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) {
            applicationsByStatus.put(s, applicationRepository.countByStatus(s));
        }

        return new PlatformAnalyticsResponse(
                companyRepository.count(),
                companyRepository.countByActiveTrue(),
                userRepository.countByRole(Role.CANDIDATE),
                userRepository.countByRole(Role.RECRUITER),
                jobRepository.count(),
                applicationRepository.count(),
                jobsByStatus,
                applicationsByStatus
        );
    }
}