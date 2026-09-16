package com.jobportal.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** "local" or "s3" */
    private String mode;
    private Local local = new Local();
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class Local {
        private String basePath;
    }

    @Getter
    @Setter
    public static class S3 {
        private String endpoint;
        private String bucket;
        private String accessKey;
        private String secretKey;
        private String region;
    }
}