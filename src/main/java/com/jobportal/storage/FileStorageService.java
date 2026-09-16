package com.jobportal.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * One interface, two implementations (local disk / S3-MinIO), selected at
 * startup by app.storage.mode. Nothing above this layer — ResumeService,
 * the controller — knows or cares which one is active. Swapping dev-local
 * storage for real S3 in production is a config change, not a code change.
 */
public interface FileStorageService {
    void store(MultipartFile file, String storageKey) throws IOException;
    byte[] load(String storageKey) throws IOException;
    void delete(String storageKey);
}