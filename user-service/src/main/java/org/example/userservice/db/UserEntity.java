package org.example.userservice.db;

import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "users",schema = "user_data")
public class UserEntity {

    @Id
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

}
