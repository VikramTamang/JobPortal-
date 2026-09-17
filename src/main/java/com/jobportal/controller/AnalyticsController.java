package com.jobportal.controller;

import com.jobportal.dto.analytics.CompanyAnalyticsResponse;
import com.jobportal.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Recruiter-facing company recruitment analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/company")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Recruitment analytics for the current recruiter's own company")
    public CompanyAnalyticsResponse getCompanyAnalytics() {
        return analyticsService.getCompanyAnalytics();
    }
}