package org.example.saga.command.compensate;

public record AwaitPaymentFailedCommand(
        String sagaId,
        Long paymentId
) {
}
