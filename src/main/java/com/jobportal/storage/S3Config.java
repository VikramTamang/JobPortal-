package com.jobportal.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/** Only registered when app.storage.mode=s3 — irrelevant (and absent) when running local storage. */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "mode", havingValue = "s3")
public class S3Config {

    private final StorageProperties storageProperties;

    @Bean
    public S3Client s3Client() {
        var s3Props = storageProperties.getS3();
        return S3Client.builder()
                .endpointOverride(URI.create(s3Props.getEndpoint()))
                .region(Region.of(s3Props.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3Props.getAccessKey(), s3Props.getSecretKey())))
                // Required for MinIO (and any non-AWS S3-compatible endpoint) --
                // without this, the SDK builds AWS's virtual-hosted-style URLs
                // (bucket.endpoint.com) which MinIO doesn't understand.
                .forcePathStyle(true)
                .build();
    }
}