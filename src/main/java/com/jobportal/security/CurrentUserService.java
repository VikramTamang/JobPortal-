package com.jobportal.security;

import com.jobportal.domain.user.Role;
import com.jobportal.exception.TenantAccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class CurrentUserService {

    public UserPrincipal getPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return principal;
    }

    /**
     * For endpoints that are publicly readable (permitAll) but still behave
     * differently for an authenticated owner — e.g. GET /jobs/{id}, where a
     * published job is visible to anyone but a draft is only visible to the
     * owning recruiter. Anonymous requests get Optional.empty() here rather
     * than an exception.
     */
    public Optional<UserPrincipal> getPrincipalIfPresent() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public Optional<UUID> getCompanyIdIfPresent() {
        return getPrincipalIfPresent().map(UserPrincipal::getCompanyId).filter(Objects::nonNull);
    }

    public UUID getUserId() {
        return getPrincipal().getId();
    }

    public Role getRole() {
        return getPrincipal().getRole();
    }

    public boolean isAdmin() {
        return getRole() == Role.ADMIN;
    }

    public boolean isRecruiter() {
        return getRole() == Role.RECRUITER;
    }

    public boolean isCandidate() {
        return getRole() == Role.CANDIDATE;
    }

    public UUID getCompanyId() {
        UUID companyId = getPrincipal().getCompanyId();
        if (companyId == null) {
            throw new TenantAccessDeniedException(
                    "This action requires a recruiter account associated with a company");
        }
        return companyId;
    }
}