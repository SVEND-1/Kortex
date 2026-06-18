package org.example.saga.command.compensate;

public record CreatePaymentFailedCommand(
        String sagaId,
        String paymentId
) {
}
