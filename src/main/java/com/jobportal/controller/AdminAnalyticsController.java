package com.jobportal.controller;

import com.jobportal.dto.analytics.PlatformAnalyticsResponse;
import com.jobportal.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Admin - Analytics", description = "Platform-wide recruitment analytics (ADMIN only)")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/platform")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Platform-wide recruitment activity and counts")
    public PlatformAnalyticsResponse getPlatformAnalytics() {
        return adminAnalyticsService.getPlatformAnalytics();
    }
}