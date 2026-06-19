package org.example.deliveryservice.api.dto.response;

import org.example.deliveryservice.db.OrderStatus;
import org.example.rest.AddressRestResponse;

import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        AddressRestResponse address,
        String message,
        List<OrderItemResponse> orderItems
) {
}
