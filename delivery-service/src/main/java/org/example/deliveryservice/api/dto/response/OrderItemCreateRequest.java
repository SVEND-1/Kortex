package org.example.deliveryservice.api.dto.response;

public record OrderItemCreateRequest(
        Long productId,
        Integer quantity
) {
}
