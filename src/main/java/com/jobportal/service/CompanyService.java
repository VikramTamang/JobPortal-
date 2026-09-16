package com.jobportal.service;

import com.jobportal.domain.company.Company;
import com.jobportal.dto.company.CompanyResponse;
import com.jobportal.dto.company.UpdateCompanyRequest;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.CompanyRepository;
import com.jobportal.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note there is no getCompany(UUID companyId) method here, deliberately.
 * A recruiter can only ever act on their OWN company — the id comes from
 * currentUserService.getCompanyId(), never from a controller parameter.
 * There is nothing for a malicious request to override.
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public CompanyResponse getMyCompany() {
        return CompanyResponse.from(loadMyCompany());
    }

    @Transactional
    public CompanyResponse updateMyCompany(UpdateCompanyRequest request) {
        Company company = loadMyCompany();
        company.setDescription(request.description());
        company.setWebsite(request.website());
        companyRepository.save(company);
        return CompanyResponse.from(company);
    }

    private Company loadMyCompany() {
        return companyRepository.findById(currentUserService.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }
}