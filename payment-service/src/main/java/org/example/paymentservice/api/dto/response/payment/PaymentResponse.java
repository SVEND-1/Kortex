package org.example.paymentservice.api.dto.response.payment;

import java.time.LocalDateTime;

public record PaymentResponse(
        String id,
        String value,
        String description,
        String status,
        LocalDateTime createdAt
) {
}
