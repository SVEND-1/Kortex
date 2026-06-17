package org.example.saga.command.compensate;

public record CancelOrderCommand(
        String sagaId,
        Long orderId
) {
}
