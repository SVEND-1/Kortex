package org.example.authservice.api.dto.response;

import org.example.kafkaEvent.Role;

public record UserRegistrationResponse(
        Long id,
        String name,
        String email,
        String password,
        Role role
) {
}
