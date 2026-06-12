package org.example.productservice.api.dto.response;

import org.example.productservice.db.Category;

public record SearchParams(
        Category category,
        int pageSize,
        int pageNumber,
        String query
) {}
