package org.example.orderservice.api.dto;

public record OrderItemCreateRequest(
        Long productId,
        Integer quantity
) {
}
