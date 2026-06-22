package org.example.adminservice.api.dto.response;

import org.example.adminservice.db.RoleRequestEntity;

import java.time.LocalDateTime;

public record RoleRequestResponse(
        Long id,
        RoleRequestEntity.Status status,
        RoleRequestEntity.TypeAction typeAction ,
        String message,
        LocalDateTime createdAt,
        Long userId,
        String name,
        String email
){
}
