package org.example.orderservice.api.dto;

import org.example.orderservice.db.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public record OrderResponseDTO(
    Long orderId,
    List<OrderItemDTO> items,
    LocalDateTime orderDate,
    BigDecimal totalAmount,
    OrderStatus status
    ){
}
