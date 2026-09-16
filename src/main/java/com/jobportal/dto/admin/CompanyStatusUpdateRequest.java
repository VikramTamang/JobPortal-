package com.jobportal.dto.admin;

import jakarta.validation.constraints.NotNull;

public record CompanyStatusUpdateRequest(
        @NotNull Boolean active
) {
}