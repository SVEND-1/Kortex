package org.example.productservice.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer count,
        String category,
        List<String> images) {
    public ProductResponse withImages(List<String> newImages) {
        return new ProductResponse(id, name, description, price, count, category, newImages);
    }
}
