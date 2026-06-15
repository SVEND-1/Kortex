package org.example.kafkaEvent;

public record UserRegisterEvent(
        Long id,
        String email,
        String name
) {
}
