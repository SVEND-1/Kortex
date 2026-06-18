package org.example.sagaorchestrator.kafka;

import lombok.RequiredArgsConstructor;
import org.example.saga.command.approve.*;
import org.example.saga.command.compensate.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static org.example.saga.KafkaTopics.*;

@RequiredArgsConstructor
@Service
public class KafkaCommand {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendReserveStock(ReserveStockCommand command) {
        kafkaTemplate.send(PRODUCT_COMMAND,command.sagaId(),command);
    }

    public void sendClearCart(ClearCartCommand command) {
        kafkaTemplate.send(CLEAR_CART_COMMAND,command.sagaId(),command);
    }

    public void sendCreatePayment(CreatePaymentCommand command) {
        kafkaTemplate.send(PAYMENT_CREATE_COMMAND,command.sagaId(),command);
    }

    public void sendUpdateOrderStatus(UpdateOrderStatusCommand command){
        kafkaTemplate.send(ORDER_STATUS_COMMAND,command.sagaId(),command);
    }


    //Компенсирующие

    public void sendReserveStockFailed(ReleaseStockCommand command){
        kafkaTemplate.send(PRODUCT_COMPENSATE_COMMAND,command.sagaId(),command);
    }

    public void sendClearCartFailed(ClearCartFailedCommand command) {
        kafkaTemplate.send(CLEAR_CART_COMPENSATE_COMMAND,command.sagaId(),command);
    }

    public void sendCreatePaymentFailed(CreatePaymentFailedCommand command) {
        kafkaTemplate.send(PAYMENT_CREATE_COMPENSATE_COMMAND,command.sagaId(),command);
    }

    public void sendPaymentFailed(PaymentFailedCommand command) {
        kafkaTemplate.send(PAYMENT_REFUND_COMMAND,command.sagaId(),command);
    }

    public void sendCancelOrder(CancelOrderCommand command){
        kafkaTemplate.send(ORDER_CANCEL_COMMAND,command.sagaId(),command);
    }
}
