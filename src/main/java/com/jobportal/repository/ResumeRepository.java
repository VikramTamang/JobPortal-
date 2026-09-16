package com.jobportal.repository;

import com.jobportal.domain.resume.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    Optional<Resume> findByIdAndCandidateId(UUID id, UUID candidateId);
    List<Resume> findByCandidateIdOrderByUploadedAtDesc(UUID candidateId);
    List<Resume> findByCandidateIdAndPrimaryTrue(UUID candidateId);
    long countByCandidateId(UUID candidateId);
}