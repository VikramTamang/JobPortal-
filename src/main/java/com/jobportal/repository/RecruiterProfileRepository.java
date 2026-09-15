package com.jobportal.repository;

import com.jobportal.domain.user.RecruiterProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, UUID> {
    Optional<RecruiterProfile> findByUserId(UUID userId);
}