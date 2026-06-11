package org.example.productservice.api.dto.response;

import java.util.List;

public record ProductPageResponse(
        List<ProductResponse> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {
}
