package org.example.paymentservice.api.dto.response.payment;

public record PaymentCreateResponse(
        String paymentId,
        String urlPay,
        Long orderId
) {
}
