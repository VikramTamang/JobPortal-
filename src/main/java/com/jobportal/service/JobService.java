package com.jobportal.service;

import com.jobportal.domain.job.Job;
import com.jobportal.domain.job.JobStatus;
import com.jobportal.domain.user.RecruiterProfile;
import com.jobportal.dto.job.JobRequest;
import com.jobportal.dto.job.JobResponse;
import com.jobportal.dto.job.JobSearchCriteria;
import com.jobportal.dto.job.JobSortOption;
import com.jobportal.exception.BadRequestException;
import com.jobportal.exception.InvalidJobStateException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.JobRepository;
import com.jobportal.repository.RecruiterProfileRepository;
import com.jobportal.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public JobResponse createJob(JobRequest request) {
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(currentUserService.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));

        Job job = new Job();
        job.setCompany(recruiter.getCompany());
        job.setCreatedByRecruiter(recruiter);
        job.setStatus(JobStatus.DRAFT);
        applyFields(job, request);

        jobRepository.save(job);
        return JobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        boolean isOwner = currentUserService.getCompanyIdIfPresent()
                .map(cid -> cid.equals(job.getCompany().getId()))
                .orElse(false);

        if (job.getStatus() != JobStatus.PUBLISHED && !isOwner) {
            throw new ResourceNotFoundException("Job not found");
        }

        return JobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> listPublishedJobs(Pageable pageable) {
        return jobRepository.findByStatus(JobStatus.PUBLISHED, pageable).map(JobResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> listMyCompanyJobs(Pageable pageable) {
        return jobRepository.findByCompanyId(currentUserService.getCompanyId(), pageable).map(JobResponse::from);
    }

    /**
     * The advanced, multi-filter candidate-facing search. Deliberately a
     * separate endpoint/method from listPublishedJobs — that one is a plain
     * browse list, this one is the keyword+filter+sort search described in
     * the spec, backed by the FULLTEXT index for keyword matching.
     */
    @Transactional(readOnly = true)
    public Page<JobResponse> search(JobSearchCriteria criteria, Pageable pageable) {
        List<String> skills = normalizeSkills(criteria.skills());
        boolean hasSkillFilter = !skills.isEmpty();

        Page<Job> jobs = jobRepository.search(
                blankToNull(criteria.keyword()),
                blankToNull(criteria.location()),
                criteria.employmentType() == null ? null : criteria.employmentType().name(),
                criteria.experienceLevel() == null ? null : criteria.experienceLevel().name(),
                criteria.workMode() == null ? null : criteria.workMode().name(),
                criteria.salaryMin(),
                criteria.salaryMax(),
                criteria.postedAfter(),
                criteria.deadlineBefore(),
                hasSkillFilter,
                hasSkillFilter ? skills : List.of("__none__"),
                sortModeOf(criteria.sortBy()),
                pageable
        );

        return jobs.map(JobResponse::from);
    }

    @Transactional
    public JobResponse updateJob(UUID jobId, JobRequest request) {
        Job job = loadOwnedJob(jobId);

        if (job.getStatus() == JobStatus.CLOSED || job.getStatus() == JobStatus.ARCHIVED) {
            throw new InvalidJobStateException(
                    "Cannot edit a job that is " + job.getStatus() + ". Only DRAFT or PUBLISHED jobs can be edited.");
        }

        applyFields(job, request);
        jobRepository.save(job);
        return JobResponse.from(job);
    }

    @Transactional
    public JobResponse publish(UUID jobId) {
        Job job = loadOwnedJob(jobId);
        requireStatus(job, JobStatus.DRAFT, JobStatus.PUBLISHED);
        job.setStatus(JobStatus.PUBLISHED);
        job.setPublishedAt(Instant.now());
        jobRepository.save(job);
        return JobResponse.from(job);
    }

    @Transactional
    public JobResponse close(UUID jobId) {
        Job job = loadOwnedJob(jobId);
        requireStatus(job, JobStatus.PUBLISHED, JobStatus.CLOSED);
        job.setStatus(JobStatus.CLOSED);
        jobRepository.save(job);
        return JobResponse.from(job);
    }

    @Transactional
    public JobResponse archive(UUID jobId) {
        Job job = loadOwnedJob(jobId);
        requireStatus(job, JobStatus.CLOSED, JobStatus.ARCHIVED);
        job.setStatus(JobStatus.ARCHIVED);
        jobRepository.save(job);
        return JobResponse.from(job);
    }

    @Transactional
    public void deleteDraft(UUID jobId) {
        Job job = loadOwnedJob(jobId);
        if (job.getStatus() != JobStatus.DRAFT) {
            throw new InvalidJobStateException(
                    "Only DRAFT jobs can be deleted. Use close/archive to retire a published job. Current status: "
                            + job.getStatus());
        }
        jobRepository.delete(job);
    }

    private Job loadOwnedJob(UUID jobId) {
        return jobRepository.findByIdAndCompanyId(jobId, currentUserService.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    private void requireStatus(Job job, JobStatus required, JobStatus target) {
        if (job.getStatus() != required) {
            throw new InvalidJobStateException(
                    "Cannot move job to " + target + " from " + job.getStatus()
                            + ". Expected current status: " + required);
        }
    }

    private void applyFields(Job job, JobRequest request) {
        if (request.salaryMin() != null && request.salaryMax() != null
                && request.salaryMax().compareTo(request.salaryMin()) < 0) {
            throw new BadRequestException("salaryMax cannot be less than salaryMin");
        }

        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setRequirements(request.requirements());
        job.setResponsibilities(request.responsibilities());
        job.setEmploymentType(request.employmentType());
        job.setExperienceLevel(request.experienceLevel());
        job.setWorkMode(request.workMode());
        job.setLocation(request.location());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        if (request.currency() != null) {
            job.setCurrency(request.currency());
        }
        if (request.vacancies() != null) {
            job.setVacancies(request.vacancies());
        }
        job.setApplicationDeadline(request.applicationDeadline());
        job.replaceSkills(request.skills());
    }

    private List<String> normalizeSkills(List<String> skills) {
        if (skills == null) {
            return List.of();
        }
        return skills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private int sortModeOf(JobSortOption option) {
        if (option == null) return 1; // default: NEWEST
        return switch (option) {
            case RELEVANCE -> 0;
            case NEWEST -> 1;
            case OLDEST -> 2;
            case SALARY_HIGH -> 3;
            case SALARY_LOW -> 4;
            case DEADLINE_SOON -> 5;
        };
    }
}