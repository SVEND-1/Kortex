package org.example.adminservice.db;


import jakarta.persistence.*;
import lombok.*;
import org.example.kafkaEvent.Role;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "role_requests",schema = "role_request")
public class RoleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_role",nullable = false)
    private Role requestedRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_action",nullable = false)
    private TypeAction typeAction;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name="status",nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "create_at",nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum TypeAction{
        REMOVE,
        ENHANCE
    }
}
