package org.example.saga;

public class KafkaTopics {

    public static String PRODUCT_COMMAND = "product-command";
    public static String PRODUCT_APPROVE_EVENT = "product-approve-event";
    public static String PRODUCT_FAILED_EVENT  = "product-failed-event";

    public static String CLEAR_CART_COMMAND = "clear-cart-command";
    public static String CLEAR_CART_APPROVE_EVENT = "clear-cart-approve-event";
    public static String CLEAR_CART_FAILED_EVENT = "clear-cart-failed-event";

    public static String PAYMENT_COMMAND  = "payment-command";
    public static String PAYMENT_APPROVE_EVENT  = "payment-approve-event";
    public static String PAYMENT_FAILED_EVENT = "payment-failed-event";

    public static String ORDER_STATUS_COMMAND  = "order-status-command";
    public static String ORDER_STATUS_APPROVE_EVENT  = "order-status-approve-event";
    public static String ORDER_STATUS_FAILED_EVENT  = "order-status-failed-event";
}
