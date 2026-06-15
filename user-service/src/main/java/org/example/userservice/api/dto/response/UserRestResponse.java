package org.example.userservice.api.dto.response;

import org.example.userservice.db.Role;

public record UserRestResponse(
        String name,
        String email,
        Role role
) {
}
