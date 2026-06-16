package org.example.paymentservice.api.exception;

public class PaymentOwnershipException extends RuntimeException {

    public PaymentOwnershipException(String message) {
        super(message);
    }
}
