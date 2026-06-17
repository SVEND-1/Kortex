package org.example.saga.event.failed;

public record OrderStatusUpdatedFailedEvent(
        String sagaId,
        String reason
) {
}
