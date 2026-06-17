package org.example.saga.command.compensate;

import org.example.saga.OrderItem;

import java.util.List;

public record ClearCartFailedCommand(
        String sagaId,
        Long userId,
        List<OrderItem> items
) {
}
