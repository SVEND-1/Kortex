package org.example.userservice.api.dto.response;

import org.example.kafkaEvent.Role;

public record UserRestResponse(
        String name,
        String email,
        Role role
) {
}
