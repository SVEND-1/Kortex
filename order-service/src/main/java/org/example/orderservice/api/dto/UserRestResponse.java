package org.example.orderservice.api.dto;

import org.example.kafkaEvent.Role;

public record UserRestResponse(
        String name,
        String email,
        Role role
) {
}
