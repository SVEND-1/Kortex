package org.example.authservice.api.dto.response;

public record LoginResponse(
        boolean success,
        String message) {
}
