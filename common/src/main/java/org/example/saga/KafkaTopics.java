package org.example.saga;

public class KafkaTopics {

    public static final String START_SAGA = "start-saga";

    public static final String PRODUCT_COMMAND = "product-command";
    public static final String PRODUCT_COMPENSATE_COMMAND = "product-compensate-command";
    public static final String PRODUCT_APPROVE_EVENT = "product-approve-event";
    public static final String PRODUCT_FAILED_EVENT  = "product-failed-event";

    public static final String CLEAR_CART_COMMAND = "clear-cart-command";
    public static final String CLEAR_CART_COMPENSATE_COMMAND = "clear-cart-compensate-command";
    public static final String CLEAR_CART_APPROVE_EVENT = "clear-cart-approve-event";
    public static final String CLEAR_CART_FAILED_EVENT = "clear-cart-failed-event";

    public static final String PAYMENT_CREATE_COMMAND = "payment-create-command";
    public static final String PAYMENT_CREATE_COMPENSATE_COMMAND = "payment-create-compensate-command";
    public static final String PAYMENT_CREATE_APPROVE_EVENT  = "payment-create-approve-event";
    public static final String PAYMENT_CREATE_FAILED_EVENT = "payment-create-failed-event";

    public static final String PAYMENT_REFUND_COMMAND = "payment-refund-command";
    public static final String PAYMENT_APPROVE_EVENT = "payment-approve-event";
    public static final String PAYMENT_FAILED_EVENT = "payment-failed-event";

    public static final String ORDER_STATUS_COMMAND  = "order-status-command";
    public static final String ORDER_STATUS_APPROVE_EVENT  = "order-status-approve-event";
    public static final String ORDER_STATUS_FAILED_EVENT  = "order-status-failed-event";

    public static final String ORDER_CANCEL_COMMAND = "order-cancel-command";
}
