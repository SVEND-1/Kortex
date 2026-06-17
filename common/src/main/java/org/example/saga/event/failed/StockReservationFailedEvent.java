package org.example.saga.event.failed;

public record StockReservationFailedEvent(
        String sagaId,
        String reason
) {
}
