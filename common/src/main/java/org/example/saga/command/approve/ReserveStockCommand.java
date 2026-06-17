package org.example.saga.command.approve;

import org.example.saga.OrderItem;

import java.util.List;

public record ReserveStockCommand(
        String sagaId,
        List<OrderItem> orderItems
) {
}
