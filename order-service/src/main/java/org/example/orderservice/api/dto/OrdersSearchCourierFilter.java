package org.example.orderservice.api.dto;


public record OrdersSearchCourierFilter(
        Integer pageSize,
        Integer pageNumber
) {
}
