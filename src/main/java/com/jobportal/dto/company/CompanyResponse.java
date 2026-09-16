package com.jobportal.dto.company;

import com.jobportal.domain.company.Company;

import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String website,
        boolean active
) {
    public static CompanyResponse from(Company c) {
        return new CompanyResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getWebsite(), c.isActive());
    }
}