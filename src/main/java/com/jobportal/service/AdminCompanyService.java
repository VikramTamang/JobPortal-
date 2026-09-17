package com.jobportal.service;

import com.jobportal.domain.company.Company;
import com.jobportal.dto.admin.CompanyAdminResponse;
import com.jobportal.dto.admin.CreateCompanyRequest;
import com.jobportal.exception.DuplicateResourceException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

    /**
     * Evicts the recruiter-facing companyInfo cache for this company --
     * without this, a recruiter whose company Admin just deactivated would
     * keep seeing active=true from cache for up to 30 minutes (the
     * companyInfo TTL), which matters here specifically because account
     * status is a security-relevant field.
     */
    @CacheEvict(cacheNames = "companyInfo", key = "#companyId")
    @Transactional
    public CompanyAdminResponse setActive(UUID companyId, boolean active) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("No company found with id " + companyId));
        company.setActive(active);
        companyRepository.save(company);
        return CompanyAdminResponse.from(company);
    }
}