package org.example.authservice.api.dto.response;

public record SimpleResponse(
        boolean success,
        String message
) {
}
