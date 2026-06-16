package org.example.sagaorchestrator.db;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "saga",schema = "saga")
public class SagaEntity {

    @Id
    private String id;

    private Long orderId;

    private Long userId;

    private Long paymentId;

    @ElementCollection
    private List<OrderItem> items;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private SagaState state;

    @ElementCollection
    private List<String> executedSteps = new ArrayList<>();

    private LocalDateTime createdAt;

    private String errorMessage;
}
