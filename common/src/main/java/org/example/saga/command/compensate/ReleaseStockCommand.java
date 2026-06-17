package org.example.saga.command.compensate;

import org.example.saga.OrderItem;

import java.util.List;

public record ReleaseStockCommand(
        String sagaId,
        List<OrderItem> items
){
}
