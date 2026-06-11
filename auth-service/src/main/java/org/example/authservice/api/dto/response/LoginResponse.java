package org.example.authservice.api.dto.response;

public record LoginResponse(
        boolean success,
        String message,
        String accessToken,
        String refreshToken) {
    public LoginResponse(boolean success, String message) {
        this(success, message, null, null);
    }
}
