package org.example.adminservice.api.dto.response;

import org.example.adminservice.db.RoleRequest;

import java.time.LocalDateTime;

public record RoleRequestResponse(
        Long id,
        RoleRequest.Status status,
        RoleRequest.TypeAction typeAction ,
        String message,
        LocalDateTime createdAt,
        Long userId,
        String name,
        String email
){
}
