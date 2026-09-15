package com.jobportal.controller;

import com.jobportal.dto.auth.AuthResponse;
import com.jobportal.dto.auth.LoginRequest;
import com.jobportal.dto.auth.RefreshTokenRequest;
import com.jobportal.dto.auth.RegisterCandidateRequest;
import com.jobportal.dto.auth.RegisterRecruiterRequest;
import com.jobportal.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, and token refresh")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/candidate")
    @Operation(summary = "Register a new candidate account")
    public ResponseEntity<AuthResponse> registerCandidate(@Valid @RequestBody RegisterCandidateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCandidate(request));
    }

    @PostMapping("/register/recruiter")
    @Operation(summary = "Register a new recruiter account under an existing company")
    public ResponseEntity<AuthResponse> registerRecruiter(@Valid @RequestBody RegisterRecruiterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerRecruiter(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive an access + refresh token pair")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a valid refresh token for a new token pair")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }
}