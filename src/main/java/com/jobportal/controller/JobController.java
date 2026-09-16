package com.jobportal.controller;

import com.jobportal.dto.job.JobRequest;
import com.jobportal.dto.job.JobResponse;
import com.jobportal.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job posting creation, editing, and workflow")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Create a new job posting (starts as DRAFT)")
    public ResponseEntity<JobResponse> create(@Valid @RequestBody JobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(request));
    }

    @GetMapping
    @Operation(summary = "List published jobs (public)")
    public Page<JobResponse> listPublished(@PageableDefault(size = 20) Pageable pageable) {
        return jobService.listPublishedJobs(pageable);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "List every job belonging to the current recruiter's company, any status")
    public Page<JobResponse> listMine(@PageableDefault(size = 20) Pageable pageable) {
        return jobService.listMyCompanyJobs(pageable);
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get a job by id — published jobs are public, others visible only to the owning company")
    public JobResponse get(@PathVariable UUID jobId) {
        return jobService.getJob(jobId);
    }

    @PutMapping("/{jobId}")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Replace a job's editable fields (DRAFT or PUBLISHED only)")
    public JobResponse update(@PathVariable UUID jobId, @Valid @RequestBody JobRequest request) {
        return jobService.updateJob(jobId, request);
    }

    @PatchMapping("/{jobId}/publish")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Publish a DRAFT job, making it publicly visible")
    public JobResponse publish(@PathVariable UUID jobId) {
        return jobService.publish(jobId);
    }

    @PatchMapping("/{jobId}/close")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Close a PUBLISHED job to new applications")
    public JobResponse close(@PathVariable UUID jobId) {
        return jobService.close(jobId);
    }

    @PatchMapping("/{jobId}/archive")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Archive a CLOSED job")
    public JobResponse archive(@PathVariable UUID jobId) {
        return jobService.archive(jobId);
    }

    @DeleteMapping("/{jobId}")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Delete a DRAFT job (published jobs must be closed/archived instead)")
    public ResponseEntity<Void> delete(@PathVariable UUID jobId) {
        jobService.deleteDraft(jobId);
        return ResponseEntity.noContent().build();
    }
}