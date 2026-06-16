package org.example.orderservice.api.dto;

public record OrderPaymentApproved(
        Long id,
        OrderResponseDTO orders,
        String paymentId,
        Boolean approved
) {
}
