package org.example.rest;

import java.math.BigDecimal;

public record ProductNoImageRestResponse(
        Long id,
        String name,
        BigDecimal price,
        String category
) {
}
