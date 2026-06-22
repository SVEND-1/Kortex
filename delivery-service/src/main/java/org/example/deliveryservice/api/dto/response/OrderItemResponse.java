package org.example.deliveryservice.api.dto.response;

import org.example.rest.ProductNoImageRestResponse;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        ProductNoImageRestResponse product,
        Integer quantity,
        BigDecimal price
) {
}
