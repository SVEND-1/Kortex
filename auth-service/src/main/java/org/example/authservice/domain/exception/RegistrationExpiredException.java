package org.example.authservice.domain.exception;

public class RegistrationExpiredException extends RuntimeException {
    public RegistrationExpiredException(String message) {
        super(message);
    }
}
