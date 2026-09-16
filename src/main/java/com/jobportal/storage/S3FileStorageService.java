package com.jobportal.storage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

/** Active when app.storage.mode=s3 — works identically against real AWS S3 or a local MinIO container. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "mode", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    /** Dev convenience: MinIO starts with no buckets, so create ours on first boot if missing. */
    @PostConstruct
    void ensureBucketExists() {
        String bucket = storageProperties.getS3().getBucket();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            } else {
                throw e;
            }
        }
    }

    @Override
    public void store(MultipartFile file, String storageKey) throws IOException {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(storageProperties.getS3().getBucket())
                        .key(storageKey)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    }

    @Override
    public byte[] load(String storageKey) throws IOException {
        try (var response = s3Client.getObject(GetObjectRequest.builder()
                .bucket(storageProperties.getS3().getBucket())
                .key(storageKey)
                .build())) {
            return response.readAllBytes();
        }
    }

    @Override
    public void delete(String storageKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(storageProperties.getS3().getBucket())
                .key(storageKey)
                .build());
    }
}