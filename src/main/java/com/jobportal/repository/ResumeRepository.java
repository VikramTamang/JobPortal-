package com.jobportal.repository;

import com.jobportal.domain.resume.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    Optional<Resume> findByIdAndCandidateId(UUID id, UUID candidateId);
}