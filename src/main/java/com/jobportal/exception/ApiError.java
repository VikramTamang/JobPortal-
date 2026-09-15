package com.jobportal.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError validation(String path, Map<String, String> validationErrors) {
        return new ApiError(Instant.now(), 400, "Validation Failed", "One or more fields are invalid", path, validationErrors);
    }
}