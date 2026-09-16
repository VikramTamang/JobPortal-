package com.jobportal.dto.resume;

public record ResumeDownloadResult(
        byte[] content,
        String fileName,
        String contentType
) {
}