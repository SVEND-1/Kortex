package org.example.orderservice.api.dto;

import java.util.List;

public record OrderPageResponse(
        List<CourierOrderDTO> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {
}
