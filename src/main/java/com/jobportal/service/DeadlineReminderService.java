package com.jobportal.service;

import com.jobportal.domain.job.Job;
import com.jobportal.domain.job.JobStatus;
import com.jobportal.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Notifies the OWNING RECRUITER (not candidates) that one of their
 * published jobs is approaching its deadline. There's no candidate
 * subscription/saved-jobs feature in this spec, so there's no candidate
 * list to notify — this gives the recruiter a chance to extend or
 * deliberately close the posting rather than have it lapse unnoticed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadlineReminderService {

    private final JobRepository jobRepository;
    private final NotificationService notificationService;
    private final NotificationProperties notificationProperties;

    @Scheduled(cron = "${app.notifications.deadline-reminder-cron:0 0 8 * * *}")
    @Transactional
    public void remindApproachingDeadlines() {
        LocalDate targetDate = LocalDate.now().plusDays(notificationProperties.getDeadlineReminderDays());
        List<Job> jobs = jobRepository.findByStatusAndApplicationDeadline(JobStatus.PUBLISHED, targetDate);

        for (Job job : jobs) {
            notificationService.enqueue(
                    job.getCreatedByRecruiter().getUser(),
                    "DEADLINE_APPROACHING",
                    "Deadline approaching: " + job.getTitle(),
                    "Your job posting \"" + job.getTitle() + "\" closes for applications on "
                            + job.getApplicationDeadline() + ". Consider extending the deadline or closing it soon.",
                    "JOB", job.getId());
        }

        if (!jobs.isEmpty()) {
            log.info("Queued {} deadline-approaching reminder(s)", jobs.size());
        }
    }
}