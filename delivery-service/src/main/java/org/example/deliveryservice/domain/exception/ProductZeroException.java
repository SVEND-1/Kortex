package org.example.deliveryservice.domain.exception;

public class ProductZeroException extends RuntimeException{
    public ProductZeroException(String message) {
        super(message);
    }
}
