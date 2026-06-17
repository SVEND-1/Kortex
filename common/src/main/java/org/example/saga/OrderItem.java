package org.example.saga;

public record OrderItem(
        Long productId,
        Integer quantity
) {
}
