package com.isi.techcenter_backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.isi.techcenter_backend.config.MinioStorageProperties;
import com.isi.techcenter_backend.error.AppErrorType;
import com.isi.techcenter_backend.error.AuthException;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;

@Service
public class MinioStorageService {

    private static final String RESEARCHER_PHOTO_PATH = "researchers/%s/photo";
    private static final String ACTUALITE_PHOTO_PATH = "actualites/%s/photo";
    private static final String PUBLICATION_PDF_PATH = "publications/%s/pdf";

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    public MinioStorageService(MinioStorageProperties properties) {
        this.properties = properties;
        this.minioClient = MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    public String storeResearcherPhoto(UUID researcherId, MultipartFile photo) {
        validateFile(photo, "photo");
        String objectName = RESEARCHER_PHOTO_PATH.formatted(researcherId);
        putObject(properties.photoBucket(), objectName, photo);
        return objectName;
    }

    public void deleteResearcherPhoto(String objectName) {
        deleteObject(properties.photoBucket(), objectName);
    }

    public String getResearcherPhotoPresignedUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }
        return getPresignedGetUrl(properties.photoBucket(), objectName);
    }

    public String storePublicationPdf(UUID publicationId, MultipartFile pdf) {
        validateFile(pdf, "pdf");
        String objectName = PUBLICATION_PDF_PATH.formatted(publicationId);
        putObject(properties.pdfBucket(), objectName, pdf);
        return objectName;
    }

    public String storeActualitePhoto(UUID actualiteId, MultipartFile photo) {
        validateFile(photo, "photo");
        String objectName = ACTUALITE_PHOTO_PATH.formatted(actualiteId);
        putObject(properties.photoBucket(), objectName, photo);
        return objectName;
    }

    public void deleteActualitePhoto(String objectName) {
        deleteObject(properties.photoBucket(), objectName);
    }

    public String getActualitePhotoPresignedUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }
        return getPresignedGetUrl(properties.photoBucket(), objectName);
    }

    public void deletePublicationPdf(String objectName) {
        deleteObject(properties.pdfBucket(), objectName);
    }

    public String getPublicationPdfPresignedUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }
        return getPresignedGetUrl(properties.pdfBucket(), objectName);
    }

    private void validateFile(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new AuthException(AppErrorType.INVALID_FILE, fieldName + " file must not be empty");
        }
    }

    private void putObject(String bucket, String objectName, MultipartFile file) {
        ensureBucketExists(bucket);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .build());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store file in MinIO", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to store file in MinIO", exception);
        }
    }

    private void deleteObject(String bucket, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to delete file from MinIO", exception);
        }
    }

    private void ensureBucketExists(String bucket) {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to ensure MinIO bucket exists", exception);
        }
    }

    private String getPresignedGetUrl(String bucket, String objectName) {
        try {
            int expirySeconds = properties.presignExpirySeconds() == null ? 3600 : properties.presignExpirySeconds();
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectName)
                    .expiry(expirySeconds, TimeUnit.SECONDS)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate MinIO presigned URL", exception);
        }
    }
}