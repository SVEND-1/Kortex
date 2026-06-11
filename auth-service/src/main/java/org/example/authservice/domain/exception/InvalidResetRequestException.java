package org.example.authservice.domain.exception;

public class InvalidResetRequestException extends RuntimeException {
    public InvalidResetRequestException(String message) {
        super(message);
    }
}
