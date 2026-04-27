package com.isi.techcenter_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.minio")
public record MinioStorageProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String photoBucket,
        String pdfBucket,
        Integer presignExpirySeconds) {
}