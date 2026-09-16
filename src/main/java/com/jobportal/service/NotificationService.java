package com.jobportal.service;

import com.jobportal.domain.notification.Notification;
import com.jobportal.domain.user.User;
import com.jobportal.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * For now this only writes a PENDING row to the notification outbox table.
 * Phase 10 adds the background worker that actually delivers these as
 * email. Keeping "decide a notification should exist" (here) separate from
 * "deliver it" (Phase 10) means the delivery mechanism can change later —
 * sync SMTP, an async queue, a different provider — without touching any
 * of the business logic that triggers notifications.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void enqueue(User recipient, String type, String subject, String body,
                        String relatedEntityType, UUID relatedEntityId) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        notificationRepository.save(notification);
    }
}