package org.example.deliveryservice.api.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        DeliveryProductResponse product,
        Integer quantity,
        BigDecimal price
) {
}
