package org.example.saga.command.approve;

import java.math.BigDecimal;

public record PaymentSuccessCommand(
        String sagaId
) {
}
