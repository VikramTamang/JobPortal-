package com.jobportal.repository;

import com.jobportal.domain.notification.Notification;
import com.jobportal.domain.notification.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    /** "Top50" bounds each poll to a manageable batch rather than pulling an unbounded backlog into memory. */
    List<Notification> findTop50ByStatusOrderByCreatedAtAsc(NotificationStatus status);
}