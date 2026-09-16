package com.jobportal.controller;

import com.jobportal.dto.company.CompanyResponse;
import com.jobportal.dto.company.UpdateCompanyRequest;
import com.jobportal.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Companies", description = "Recruiter self-service for their own company")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Get the current recruiter's own company")
    public CompanyResponse getMyCompany() {
        return companyService.getMyCompany();
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Update the current recruiter's own company (description/website only)")
    public CompanyResponse updateMyCompany(@Valid @RequestBody UpdateCompanyRequest request) {
        return companyService.updateMyCompany(request);
    }
}