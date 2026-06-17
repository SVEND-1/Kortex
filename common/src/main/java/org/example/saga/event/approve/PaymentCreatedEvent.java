package org.example.saga.event.approve;

public record PaymentCreatedEvent(
        String sagaId,
        Long paymentId
) {
}
