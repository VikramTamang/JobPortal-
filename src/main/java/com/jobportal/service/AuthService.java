package com.jobportal.service;

import com.jobportal.domain.company.Company;
import com.jobportal.domain.user.CandidateProfile;
import com.jobportal.domain.user.RecruiterProfile;
import com.jobportal.domain.user.Role;
import com.jobportal.domain.user.User;
import com.jobportal.dto.auth.AuthResponse;
import com.jobportal.dto.auth.LoginRequest;
import com.jobportal.dto.auth.RegisterCandidateRequest;
import com.jobportal.dto.auth.RegisterRecruiterRequest;
import com.jobportal.exception.EmailAlreadyExistsException;
import com.jobportal.exception.InvalidCredentialsException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.CandidateProfileRepository;
import com.jobportal.repository.CompanyRepository;
import com.jobportal.repository.RecruiterProfileRepository;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.JwtProperties;
import com.jobportal.security.JwtService;
import com.jobportal.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @Transactional
    public AuthResponse registerCandidate(RegisterCandidateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.CANDIDATE);
        user.setActive(true);
        userRepository.save(user);

        CandidateProfile profile = new CandidateProfile();
        profile.setUser(user);
        profile.setFullName(request.fullName());
        profile.setPhone(request.phone());
        candidateProfileRepository.save(profile);

        auditService.recordForActor(user, "USER_REGISTERED", "USER", user.getId());

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse registerRecruiter(RegisterRecruiterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        Company company = companyRepository.findBySlug(request.companySlug())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No company found with slug '" + request.companySlug() + "'"));

        if (!company.isActive()) {
            throw new ResourceNotFoundException("Company is not currently active");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.RECRUITER);
        user.setCompany(company);
        user.setActive(true);
        userRepository.save(user);

        RecruiterProfile profile = new RecruiterProfile();
        profile.setUser(user);
        profile.setCompany(company);
        profile.setFullName(request.fullName());
        profile.setJobTitle(request.jobTitle());
        recruiterProfileRepository.save(profile);

        auditService.recordForActor(user, "USER_REGISTERED", "USER", user.getId());

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        auditService.recordForActor(user, "LOGIN", "USER", user.getId());

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        User user = refreshTokenService.consumeAndRotate(rawRefreshToken);
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = refreshTokenService.issue(user);

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpirationMs(),
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }
}