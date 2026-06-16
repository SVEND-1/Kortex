package org.example.kafkaEvent;

public record RoleUpdateEvent(
        Long id,
        Role updatedRole
) {
}
