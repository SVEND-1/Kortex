package org.example.sagaorchestrator.db;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEntity {
    private Long productId;
    private Integer quantity;
}
