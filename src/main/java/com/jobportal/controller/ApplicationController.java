package com.jobportal.controller;

import com.jobportal.domain.application.ApplicationStatus;
import com.jobportal.dto.application.ApplicationResponse;
import com.jobportal.dto.application.ApplicationStatusHistoryResponse;
import com.jobportal.dto.application.ApplyToJobRequest;
import com.jobportal.dto.application.UpdateApplicationStatusRequest;
import com.jobportal.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Candidate applications and the recruiter review pipeline")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Apply to a job")
    public ResponseEntity<ApplicationResponse> apply(@Valid @RequestBody ApplyToJobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.apply(request));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "List the current candidate's own applications")
    public Page<ApplicationResponse> listMine(@PageableDefault(size = 20) Pageable pageable) {
        return applicationService.listMyApplications(pageable);
    }

    @GetMapping("/company")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "List every application across the recruiter's company, optionally filtered by status")
    public Page<ApplicationResponse> listForCompany(
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return applicationService.listForCompany(status, pageable);
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('CANDIDATE','RECRUITER')")
    @Operation(summary = "Get one application (candidate: own only; recruiter: own company only)")
    public ApplicationResponse get(@PathVariable UUID applicationId) {
        return applicationService.getForCurrentUser(applicationId);
    }

    @GetMapping("/{applicationId}/history")
    @PreAuthorize("hasAnyRole('CANDIDATE','RECRUITER')")
    @Operation(summary = "Get the full status history / audit trail for an application")
    public List<ApplicationStatusHistoryResponse> history(@PathVariable UUID applicationId) {
        return applicationService.getHistory(applicationId);
    }

    @PatchMapping("/{applicationId}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Move an application through the pipeline (or reject it)")
    public ApplicationResponse updateStatus(
            @PathVariable UUID applicationId,
            @Valid @RequestBody UpdateApplicationStatusRequest request) {
        return applicationService.updateStatus(applicationId, request);
    }

    @PostMapping("/{applicationId}/withdraw")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Withdraw your own application")
    public ApplicationResponse withdraw(@PathVariable UUID applicationId) {
        return applicationService.withdraw(applicationId);
    }
}