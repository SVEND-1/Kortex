package org.example.cartservice.api.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String image,
        BigDecimal price,
        Integer quantity
) {

}