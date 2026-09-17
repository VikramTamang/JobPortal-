package com.jobportal.dto.analytics;

import java.util.UUID;

public record JobApplicationCount(UUID jobId, String jobTitle, Long applicationCount) {
}