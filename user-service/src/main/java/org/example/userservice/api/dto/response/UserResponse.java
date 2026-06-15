package org.example.userservice.api.dto.response;

import org.example.userservice.db.Address;
import org.example.userservice.db.Role;

public record UserResponse(
        Long userId,
        String email,
        String name,
        Address address,
        Role role
) {
}
