package org.example.authservice.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotNull
        String resetId,
        @NotNull
        @Size(min = 6, max = 50)
        String newPassword,
        @NotNull
        @Size(min = 6, max = 50)
        String confirmPassword
) {
}
