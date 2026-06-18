package org.example.orderservice.api.dto;

import java.util.List;

public record OrderCreateRequest(
        String comment,
        List<OrderItemCreateRequest> request
) {
}
