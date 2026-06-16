package org.example.orderservice.db;

public enum OrderStatus {
    AWAITING_PAYMENT,
    PENDING,
    DISPATCHED,
    DELIVERED_TO_DESTINATION,
    CANCELLED,
    RETURNED,
    COMPLETED
}
