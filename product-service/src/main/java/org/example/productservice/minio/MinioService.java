package org.example.productservice.minio;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productservice.config.MinioConfig;
import org.example.productservice.minio.exceptions.MinioServiceException;
import org.example.productservice.minio.utils.FileValidator;
import org.example.productservice.minio.utils.PathUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final FileValidator fileValidator;

    @Value("${minio.buckets.product-images}")
    private String MODULE_KEY;

    public String uploadFile(
            MultipartFile file
    ) {
        fileValidator.validate(file);

        String bucketName = minioConfig.getBuckets().get(MODULE_KEY);
        if (bucketName == null) {
            throw new MinioServiceException("Неизвестный ключ модуля: " + MODULE_KEY);
        }

        String objectPath = PathUtil.buildPath(MODULE_KEY, file);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Файл успешно загружен в бакет {} по пути {}", bucketName, objectPath);
            return objectPath;
        } catch (Exception e) {
            log.error("Не удалось загрузить файл", e);
            throw new MinioServiceException("Не удалось загрузить файл: " + e.getMessage(), e);
        }
    }

    public String overwriteFile(String objectPath, MultipartFile file) {
        fileValidator.validate(file);

        String bucketName = minioConfig.getBuckets().get(MODULE_KEY);
        if (bucketName == null) {
            throw new MinioServiceException("Неизвестный ключ модуля: " + MODULE_KEY);
        }

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Файл перезаписан в бакете {} по пути {}", bucketName, objectPath);
            return objectPath;
        } catch (Exception e) {
            log.error("Не удалось перезаписать файл по пути {}", objectPath, e);
            throw new MinioServiceException("Не удалось перезаписать файл: " + e.getMessage(), e);
        }
    }

    public String generatePresignedUrl(String objectPath) {
        String bucketName = minioConfig.getBuckets().get(MODULE_KEY);
        if (bucketName == null) {
            throw new MinioServiceException("Неизвестный ключ модуля: " + MODULE_KEY);
        }

        Integer expirySeconds = minioConfig.getExpiry().get(MODULE_KEY);
        if (expirySeconds == null) {
            expirySeconds = minioConfig.getDefaultExpirySeconds();
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .method(Method.GET)
                            .expiry(expirySeconds)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioServiceException("Не удалось сгенерировать подписанную ссылку", e);
        }
    }

    public void deleteFile(String objectPath) {
        String bucketName = minioConfig.getBuckets().get(MODULE_KEY);
        if (bucketName == null) {
            throw new MinioServiceException("Неизвестный ключ модуля: " + MODULE_KEY);
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .build()
            );
            log.info("Файл удалён из бакета '{}', путь '{}'", bucketName, objectPath);
        } catch (Exception e) {
            log.error("Не удалось удалить файл из MinIO", e);
            throw new MinioServiceException("Не удалось удалить файл: " + e.getMessage(), e);
        }
    }
}