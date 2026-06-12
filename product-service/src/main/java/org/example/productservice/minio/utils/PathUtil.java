package org.example.productservice.minio.utils;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public final class PathUtil {

    private PathUtil() {

    }

    public static String buildPath(String module, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueName = UUID.randomUUID().toString() + extension;
        return String.format("%s/%s", module, uniqueName);
    }
}
