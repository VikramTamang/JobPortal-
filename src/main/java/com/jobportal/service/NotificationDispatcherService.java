package com.jobportal.service;

import com.jobportal.domain.notification.Notification;
import com.jobportal.domain.notification.NotificationStatus;
import com.jobportal.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcherService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Scheduled(fixedDelayString = "${app.notifications.dispatch-interval-ms:15000}")
    @Transactional
    public void dispatchPending() {
        List<Notification> batch = notificationRepository.findTop50ByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING);

        for (Notification notification : batch) {
            try {
                emailService.send(
                        notification.getRecipient().getEmail(),
                        notification.getSubject(),
                        notification.getBody());
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
            } catch (Exception e) {
                // No automatic retry in this phase -- a FAILED row stays
                // FAILED. Admin-visible retry/requeue is a natural
                // extension once Phase 11's admin views exist to trigger
                // it from.
                notification.setStatus(NotificationStatus.FAILED);
                log.error("Failed to send notification {} to {}: {}",
                        notification.getId(), notification.getRecipient().getEmail(), e.getMessage());
            }
            notificationRepository.save(notification);
        }

        if (!batch.isEmpty()) {
            log.info("Dispatched {} notification(s)", batch.size());
        }
    }
}