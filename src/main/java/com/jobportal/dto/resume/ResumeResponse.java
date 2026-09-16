package com.jobportal.dto.resume;

import com.jobportal.domain.resume.Resume;

import java.time.Instant;
import java.util.UUID;

public record ResumeResponse(
        UUID id,
        String fileName,
        String contentType,
        long fileSizeBytes,
        boolean primary,
        Instant uploadedAt
) {
    public static ResumeResponse from(Resume r) {
        return new ResumeResponse(r.getId(), r.getFileName(), r.getContentType(),
                r.getFileSizeBytes(), r.isPrimary(), r.getUploadedAt());
    }
}