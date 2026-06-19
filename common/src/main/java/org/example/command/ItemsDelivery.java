package org.example.command;

public record ItemsDelivery(
        Long itemId,
        Long productId,
        Integer quantity
) {
}
