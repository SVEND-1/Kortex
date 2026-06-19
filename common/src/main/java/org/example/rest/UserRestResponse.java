package org.example.rest;

import org.example.kafkaEvent.Role;

public record UserRestResponse(
        String name,
        String email,
        Role role
) {
}
