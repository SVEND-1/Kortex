package org.example.paymentservice.domain.exception;

public class PaymentOwnershipException extends RuntimeException {

    public PaymentOwnershipException(String message) {
        super(message);
    }
}
