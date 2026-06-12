package org.example.productservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productservice.minio.MinioService;
import org.example.productservice.minio.exceptions.MinioServiceException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductImageService {
    private final MinioService minioService;

    public List<String> findImages(List<String> imagePaths) {
        List<String> imagePath = new ArrayList<>();
        for (String productPath : imagePaths) {
            try {
                String path = minioService.generatePresignedUrl(productPath);
                imagePath.add(path);
            } catch (MinioServiceException e) {
                log.error("Ошибка получения presigned URL для пути: {}", productPath, e);
                throw new RuntimeException("Не удалось получить ссылки на изображения", e);
            }
        }
        return imagePath;
    }

    public List<String> saveImages(List<MultipartFile> imageFiles) {
        List<String> imagePaths = new ArrayList<>();
        for (MultipartFile file : imageFiles) {
            try {
                String path = minioService.uploadFile(file);
                imagePaths.add(path);
            } catch (MinioServiceException e) {
                log.error("Ошибка загрузки файла: {}", file.getOriginalFilename(), e);
                throw new RuntimeException("Не удалось сохранить изображения", e);
            }
        }
        return imagePaths;
    }

    public List<String> updateImages(List<String> imagePaths, List<MultipartFile> imageFiles) {
        if (imagePaths.size() != imageFiles.size()) {
            throw new IllegalArgumentException("Списки путей и файлов должны быть одинакового размера");
        }

        List<String> updatedImagePaths = new ArrayList<>();
        for (int i = 0; i < imageFiles.size(); i++) {
            try {
                String path = minioService.overwriteFile(imagePaths.get(i), imageFiles.get(i));
                updatedImagePaths.add(path);
            } catch (MinioServiceException e) {
                log.error("Ошибка обновления файла по пути: {}", imagePaths.get(i), e);
                throw new RuntimeException("Не удалось обновить изображения", e);
            }
        }
        return updatedImagePaths;
    }

    public void deleteImages(List<String> imagesPath) {
        for (String path : imagesPath) {
            try {
                minioService.deleteFile(path);
            } catch (MinioServiceException e) {
                log.error("Ошибка удаления файла по пути: {}", path, e);
                throw new RuntimeException("Не удалось удалить изображения", e);
            }
        }
    }
}