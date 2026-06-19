package org.example.deliveryservice.db;

public enum OrderStatus {
    CREATED,
    PENDING,
    DISPATCHED,
    DELIVERED_TO_DESTINATION,
    CANCELLED,
    RETURNED,
    COMPLETED
}
