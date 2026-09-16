package com.jobportal.controller;

import com.jobportal.dto.resume.ResumeDownloadResult;
import com.jobportal.dto.resume.ResumeResponse;
import com.jobportal.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
@Tag(name = "Resumes", description = "Resume upload, management, and tenant-safe access")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Upload a resume (PDF/DOC/DOCX, max 5MB)")
    public ResponseEntity<ResumeResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean primary) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeService.upload(file, primary));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "List the current candidate's own resumes")
    public List<ResumeResponse> listMine() {
        return resumeService.listMine();
    }

    @PatchMapping("/{resumeId}/primary")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Mark a resume as the primary one")
    public ResumeResponse setPrimary(@PathVariable UUID resumeId) {
        return resumeService.setPrimary(resumeId);
    }

    @DeleteMapping("/{resumeId}")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Delete a resume (blocked if it's attached to an existing application)")
    public ResponseEntity<Void> delete(@PathVariable UUID resumeId) {
        resumeService.delete(resumeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{resumeId}/download")
    @PreAuthorize("hasAnyRole('CANDIDATE','RECRUITER')")
    @Operation(summary = "Download a resume — candidate: own only; recruiter: only if the candidate applied to their company's job")
    public ResponseEntity<byte[]> download(@PathVariable UUID resumeId) {
        ResumeDownloadResult result = resumeService.download(resumeId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(result.fileName())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }
}