package org.example.saga.event.approve;

public record PaymentSuccessEvent(
        String sagaId
) {
}
