package org.example.authservice.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record VerifyRegisterRequest(
        @NotNull
        String registrationId,
        @NotNull
        String code
) {
}
