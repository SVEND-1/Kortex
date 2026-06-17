package org.example.sagaorchestrator.db;

public enum SagaStep {
    RESERVE_STOCK,
    CLEAR_CART,
    CREATE_PAYMENT,
    PAYMENT_SUCCESS,
    UPDATE_ORDER_STATUS
}
