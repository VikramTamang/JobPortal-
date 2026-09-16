package com.jobportal.service;

import com.jobportal.domain.company.Company;
import com.jobportal.dto.admin.CompanyAdminResponse;
import com.jobportal.dto.admin.CreateCompanyRequest;
import com.jobportal.exception.DuplicateResourceException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Unlike CompanyService/RecruiterProfileService, this one DOES take a
 * companyId parameter — because ADMIN is the one role explicitly permitted
 * to act across every tenant. Access to this whole service is gated by
 * @PreAuthorize("hasRole('ADMIN')") at the controller layer.
 */
@Service
@RequiredArgsConstructor
public class AdminCompanyService {

    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public Page<CompanyAdminResponse> listCompanies(Pageable pageable) {
        return companyRepository.findAll(pageable).map(CompanyAdminResponse::from);
    }

    @Transactional
    public CompanyAdminResponse createCompany(CreateCompanyRequest request) {
        if (companyRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("A company with slug '" + request.slug() + "' already exists");
        }

        Company company = new Company();
        company.setName(request.name());
        company.setSlug(request.slug());
        company.setDescription(request.description());
        company.setWebsite(request.website());
        company.setActive(true);
        companyRepository.save(company);

        return CompanyAdminResponse.from(company);
    }

    @Transactional
    public CompanyAdminResponse setActive(UUID companyId, boolean active) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("No company found with id " + companyId));
        company.setActive(active);
        companyRepository.save(company);
        return CompanyAdminResponse.from(company);
    }
}