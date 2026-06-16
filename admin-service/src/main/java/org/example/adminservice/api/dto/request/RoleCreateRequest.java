package org.example.adminservice.api.dto.request;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.adminservice.db.RoleRequest;
import org.example.kafkaEvent.Role;

public record RoleCreateRequest(
        @NotNull
        Role requestedRole,
        @NotNull
        RoleRequest.TypeAction typeAction,
        @NotNull
        @Size(min = 20, max = 500)
        String message
) {
}
