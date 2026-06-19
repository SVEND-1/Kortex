package org.example.deliveryservice.api.dto.response;

import java.util.List;

public record OrderPageResponse(
        List<OrderResponse> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {
}

