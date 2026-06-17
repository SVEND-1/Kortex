package org.example.saga.command.approve;

import org.example.saga.OrderItem;

import java.util.List;

public record ClearCartCommand(
        String sagaId,
        Long userId,
        List<OrderItem> orderItems
) {
}
