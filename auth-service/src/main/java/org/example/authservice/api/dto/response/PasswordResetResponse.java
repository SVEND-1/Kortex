package org.example.authservice.api.dto.response;

public record PasswordResetResponse(
        boolean success,
        String message,
        String resetId
) {
}
