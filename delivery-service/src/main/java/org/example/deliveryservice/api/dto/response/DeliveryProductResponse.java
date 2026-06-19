package org.example.deliveryservice.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DeliveryProductResponse(
        Long id,
        String name,
        BigDecimal price,
        String category,
        List<String> images
) {
}
