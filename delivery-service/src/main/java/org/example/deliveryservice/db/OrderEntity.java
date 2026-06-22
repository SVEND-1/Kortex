package org.example.deliveryservice.db;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "orders",schema = "delivery")
public class OrderEntity {
    @Id
    private Long id;

    @JoinColumn(name = "user_id", nullable = false)
    private Long userId;

    @JoinColumn(name = "courier_id")
    private Long courierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status = OrderStatus.AWAIT_COURIER;

    @Column(name = "courier_taken")
    private LocalDateTime courierTaken;

    @Embedded
    private Address address;

    @Column(name = "message",length = 500)
    private String message;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItemEntity> orderItems = new ArrayList<>();

}
