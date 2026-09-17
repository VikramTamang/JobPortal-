package com.jobportal.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.notifications")
public class NotificationProperties {
    private String fromAddress;
    private long dispatchIntervalMs = 15000;
    private int deadlineReminderDays = 3;
}