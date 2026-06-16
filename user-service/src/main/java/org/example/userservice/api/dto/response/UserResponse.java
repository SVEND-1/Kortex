package org.example.userservice.api.dto.response;

import org.example.kafkaEvent.Role;
import org.example.userservice.db.Address;

public record UserResponse(
        Long userId,
        String email,
        String name,
        Address address,
        Role role
) {
}
