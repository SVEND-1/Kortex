package org.example.saga.command.approve;

public record PaymentAwaitEvent(
        String sagaId
) {
}
