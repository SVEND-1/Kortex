package org.example.deliveryservice.db;

public enum OrderStatus {
    CREATED,
    AWAIT_COURIER,
    DISPATCHED,
    DELIVERED_TO_DESTINATION,
    CANCELLED,
    RETURNED,
    COMPLETED
}
