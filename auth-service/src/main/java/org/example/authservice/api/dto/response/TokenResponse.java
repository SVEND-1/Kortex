package org.example.authservice.api.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}
