package org.example.deliveryservice.api.dto.request;


public record OrdersSearchCourierFilter(
        Integer pageSize,
        Integer pageNumber
) {
}
