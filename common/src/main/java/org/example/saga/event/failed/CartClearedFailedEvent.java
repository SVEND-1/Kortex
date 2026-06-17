package org.example.saga.event.failed;

public record CartClearedFailedEvent(
        String sagaId,
        String reason
) {
}
