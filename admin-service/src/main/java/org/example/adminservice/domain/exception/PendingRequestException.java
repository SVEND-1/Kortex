package org.example.adminservice.domain.exception;

public class PendingRequestException extends RuntimeException {
    public PendingRequestException(String message) {
        super(message);
    }
}
