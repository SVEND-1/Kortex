package org.example.adminservice.api.dto.request;


import org.example.adminservice.db.Role;
import org.example.adminservice.db.RoleRequest;

public record RoleRequestFilter(
        Role role,
        RoleRequest.Status status,
        RoleRequest.TypeAction actionType,
        Integer pageSize,
        Integer pageNumber
) {
}
