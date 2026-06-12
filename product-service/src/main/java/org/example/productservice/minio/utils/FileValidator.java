package org.example.productservice.minio.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productservice.minio.exceptions.MinioServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileValidator {
    @Value("${minio.validation.max-file-size}")
    private String maxFileSize;

    @Value("${minio.validation.allowed-types}")
    private List<String> allowedTypes;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MinioServiceException("Файл пуст");
        }

        long maxSize = parseSize(maxFileSize);
        if (file.getSize() > maxSize) {
            throw new MinioServiceException(
                    String.format("Размер файла превышает лимит %s", maxFileSize)
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new MinioServiceException(
                    String.format("Тип файла '%s' не разрешён. Разрешённые: %s",
                            contentType, allowedTypes));
        }
    }

    private long parseSize(String size) {
        size = size.toUpperCase();
        if (size.endsWith("KB")) {
            return Long.parseLong(size.replace("KB", "").trim()) * 1024L;
        } else if (size.endsWith("MB")) {
            return Long.parseLong(size.replace("MB", "").trim()) * 1024L * 1024L;
        } else if (size.endsWith("GB")) {
            return Long.parseLong(size.replace("GB", "").trim()) * 1024L * 1024L * 1024L;
        } else {
            return Long.parseLong(size.trim());
        }
    }
}

