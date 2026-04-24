package org.example.authservice.api.dto.response;

public record RegistrationResponse(
        boolean success,
        String message,
        String registrationId
) {
}
