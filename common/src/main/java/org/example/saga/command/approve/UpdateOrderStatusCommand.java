package org.example.saga.command.approve;

public record UpdateOrderStatusCommand(
        String sagaId,
        Long orderId
) {
}
