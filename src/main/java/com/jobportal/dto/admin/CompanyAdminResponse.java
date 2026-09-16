package com.jobportal.dto.admin;

import com.jobportal.domain.company.Company;

import java.time.Instant;
import java.util.UUID;

public record CompanyAdminResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String website,
        boolean active,
        Instant createdAt
) {
    public static CompanyAdminResponse from(Company c) {
        return new CompanyAdminResponse(
                c.getId(), c.getName(), c.getSlug(), c.getDescription(),
                c.getWebsite(), c.isActive(), c.getCreatedAt());
    }
}