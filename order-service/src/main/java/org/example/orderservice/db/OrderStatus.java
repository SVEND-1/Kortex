package org.example.orderservice.db;

public enum OrderStatus {
    CREATED,
    AWAITING_PAYMENT,
    FAILED_PAYMENT,
    PENDING,
    DISPATCHED,
    DELIVERED_TO_DESTINATION,
    CANCELLED,
    RETURNED,
    COMPLETED
}
