package org.example.saga.event.failed;

public record PaymentFailedEvent(
        String sagaId,
        String reason
) {
}
