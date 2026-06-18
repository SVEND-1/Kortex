package org.example.saga.event;

import org.example.saga.OrderItem;

import java.math.BigDecimal;
import java.util.List;

public record StartSagaEvent(
        Long userId,
        Long orderId,
        List<OrderItem> orderItems,
        BigDecimal totalAmount
) {
}
