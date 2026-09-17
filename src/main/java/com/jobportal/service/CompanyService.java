package com.jobportal.service;

import com.jobportal.domain.company.Company;
import com.jobportal.dto.company.CompanyResponse;
import com.jobportal.dto.company.UpdateCompanyRequest;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.CompanyRepository;
import com.jobportal.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CurrentUserService currentUserService;

    @Cacheable(cacheNames = "companyInfo", key = "#root.target.currentUserService.getCompanyId()")
    @Transactional(readOnly = true)
    public CompanyResponse getMyCompany() {
        return CompanyResponse.from(loadMyCompany());
    }

    @CacheEvict(cacheNames = "companyInfo", key = "#root.target.currentUserService.getCompanyId()")
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