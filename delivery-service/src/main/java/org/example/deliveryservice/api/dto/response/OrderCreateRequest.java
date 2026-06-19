package org.example.deliveryservice.api.dto.response;

import java.util.List;

public record OrderCreateRequest(
        String comment,
        List<OrderItemCreateRequest> request
) {
}
