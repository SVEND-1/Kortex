package org.example.orderservice.domain.exception;

public class ProductZeroException extends RuntimeException{
    public ProductZeroException(String message) {
        super(message);
    }
}
