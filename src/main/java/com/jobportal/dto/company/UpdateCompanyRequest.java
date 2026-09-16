package com.jobportal.dto.company;

import jakarta.validation.constraints.Size;

/** name and slug are deliberately absent — only Admin can rename a tenant. */
public record UpdateCompanyRequest(
        @Size(max = 2000) String description,
        @Size(max = 500) String website
) {
}