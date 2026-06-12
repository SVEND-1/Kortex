package org.example.productservice.api;

import lombok.RequiredArgsConstructor;
import org.example.productservice.minio.MinioService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class TestMinioController {

    private final MinioService minioService;

    @GetMapping("/find")
    public ResponseEntity<String> getFile(@RequestParam String filePath) {
        return ResponseEntity.ok(minioService.generatePresignedUrl(filePath));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file) {
        String objectPath = minioService.uploadFile(file);
        return ResponseEntity.ok(objectPath);
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateFile(
            @RequestParam String objectPath,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok( minioService.overwriteFile(objectPath, file));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteFile(@RequestParam String objectPath) {
        minioService.deleteFile(objectPath);
        return ResponseEntity.noContent().build();
    }
}