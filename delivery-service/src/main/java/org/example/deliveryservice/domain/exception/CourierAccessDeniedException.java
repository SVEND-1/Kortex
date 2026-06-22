package org.example.deliveryservice.domain.exception;

public class CourierAccessDeniedException extends RuntimeException {
    public CourierAccessDeniedException(String message) {
        super(message);
    }
}
