package org.example.saga.command.approve;

public record AwaitPaymentCommand(
        String sagaId
) {
}
