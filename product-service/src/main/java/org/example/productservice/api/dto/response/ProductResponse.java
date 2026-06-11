package org.example.productservice.api.dto.response;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer count,
        String category,
        String image) {
}
