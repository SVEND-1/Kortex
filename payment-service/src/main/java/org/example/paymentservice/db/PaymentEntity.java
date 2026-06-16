package org.example.paymentservice.db;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "payments")
public class PaymentEntity {
    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "payment_id")
    private String paymentId;//yookassa

    @Column(name = "receipt_id")
    private String receiptId;

    @Column(name = "paid")
    private Boolean paid;

    @Column(name = "amount")
    private BigDecimal amount;

    @JoinColumn(name = "user_id")
    private Long userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;
}