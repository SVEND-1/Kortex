package org.example.cartservice.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(//ВЫНЕСТИ В Common
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer count,
        String category,
        List<String> images
) {}