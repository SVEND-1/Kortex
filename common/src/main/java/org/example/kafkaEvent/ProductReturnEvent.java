package org.example.kafkaEvent;

import java.util.Map;

public record ProductReturnEvent(
        Map<Long,Integer> productIdAndQuantity
) {
}
