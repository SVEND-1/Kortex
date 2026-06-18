package org.example.saga.command.compensate;

public record PaymentFailedCommand(
        String sagaId,
        String paymentId
) {
}
