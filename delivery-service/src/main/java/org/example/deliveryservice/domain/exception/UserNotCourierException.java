package org.example.deliveryservice.domain.exception;

public class UserNotCourierException extends IllegalArgumentException{
    public UserNotCourierException(String message) {
        super(message);
    }
}
