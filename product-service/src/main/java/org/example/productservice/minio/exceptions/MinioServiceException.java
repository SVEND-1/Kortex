package org.example.productservice.minio.exceptions;

public class MinioServiceException extends RuntimeException {
    public MinioServiceException(String message) {
        super(message);
    }
    public MinioServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
