package com.jobportal.dto.auth;

import com.jobportal.domain.user.Role;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UUID userId,
        String email,
        Role role
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresInMs,
                                  UUID userId, String email, Role role) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInMs / 1000, userId, email, role);
    }
}