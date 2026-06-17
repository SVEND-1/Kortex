package org.example.saga.event.approve;

public record OrderStatusUpdatedEvent(
        String sagaId
) {
}
