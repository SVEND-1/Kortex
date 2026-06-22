package org.example.adminservice.api.dto.request;

import org.example.adminservice.db.RoleRequestEntity;
import org.example.kafkaEvent.Role;

public record RoleRequestFilter(
        Role role,
        RoleRequestEntity.Status status,
        RoleRequestEntity.TypeAction actionType,
        Integer pageSize,
        Integer pageNumber
) {
}
