package com.jobportal.controller;

import com.jobportal.dto.admin.CompanyAdminResponse;
import com.jobportal.dto.admin.CompanyStatusUpdateRequest;
import com.jobportal.dto.admin.CreateCompanyRequest;
import com.jobportal.service.AdminCompanyService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/companies")
@RequiredArgsConstructor
@Tag(name = "Admin - Companies", description = "Platform-wide tenant management (ADMIN only)")
public class AdminCompanyController {

    private final AdminCompanyService adminCompanyService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List every company on the platform")
    public Page<CompanyAdminResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return adminCompanyService.listCompanies(pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new company (tenant)")
    public ResponseEntity<CompanyAdminResponse> create(@Valid @RequestBody CreateCompanyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCompanyService.createCompany(request));
    }

    @PatchMapping("/{companyId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or deactivate a company")
    public CompanyAdminResponse setStatus(
            @PathVariable UUID companyId,
            @Valid @RequestBody CompanyStatusUpdateRequest request) {
        return adminCompanyService.setActive(companyId, request.active());
    }
}