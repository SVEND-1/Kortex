package org.example.orderservice.api.dto;


public record OrderItemDTO(
    String nameProduct,
    Double priceProduct,
    int count,
    String image
    ){
}

