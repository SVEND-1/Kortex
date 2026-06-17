package org.example.sagaorchestrator.dto;

import org.example.saga.OrderItem;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreateRequest(//TODO в коммон
        Long userId,
        Long orderId,
        List<OrderItem> orderItems,
        BigDecimal totalAmount
) {
}
