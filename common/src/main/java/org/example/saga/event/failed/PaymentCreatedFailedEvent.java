package org.example.saga.event.failed;

public record PaymentCreatedFailedEvent(
        String sagaId,
        String reason
) {
}
