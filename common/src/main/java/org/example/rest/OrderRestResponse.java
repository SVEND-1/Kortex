package org.example.rest;

import java.math.BigDecimal;

public record OrderRestResponse(
        BigDecimal price,
        Integer quantity,
        Long productId
) {
}
