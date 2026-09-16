package com.jobportal.dto.application;

import com.jobportal.domain.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateApplicationStatusRequest(
        @NotNull ApplicationStatus status,
        @Size(max = 2000) String note
) {
}