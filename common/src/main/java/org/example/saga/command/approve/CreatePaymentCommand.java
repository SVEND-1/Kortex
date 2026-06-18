package org.example.saga.command.approve;

import java.math.BigDecimal;

public record CreatePaymentCommand(
        String sagaId,
        Long orderId,
        Long userID,
        BigDecimal amount
) {
}
