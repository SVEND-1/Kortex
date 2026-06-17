package org.example.saga.command.compensate;

public record UpdateOrderStatusFailedCommand(
        String sagaId,
        Long orderId
) {
}
