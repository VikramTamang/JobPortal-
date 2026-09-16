package com.jobportal.dto.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ApplyToJobRequest(
        @NotNull UUID jobId,
        UUID resumeId,
        @Size(max = 5000) String coverLetter
) {
}