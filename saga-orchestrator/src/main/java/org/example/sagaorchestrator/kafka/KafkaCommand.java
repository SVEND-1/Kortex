package org.example.sagaorchestrator.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.example.saga.command.approve.*;
import org.example.saga.command.compensate.*;

import static org.example.saga.KafkaTopics.PRODUCT_COMMAND;

public class KafkaCommand {

    private final KafkaProducer kafkaProducer;

    public void sendReserveStock(ReserveStockCommand command) {
        //Отправить в топис команды в product сервис
        kafkaProducer.send(PRODUCT_COMMAND,command.sagaId(),command);
    }

    public void sendClearCart(ClearCartCommand command) {

    }

    public void sendCreatePayment(CreatePaymentCommand command) {

    }

    public void sendAwaitPayment(AwaitPaymentCommand command) {

    }

    public void sendUpdateOrderStatus(UpdateOrderStatusCommand command){

    }


    //Компенсирующие

    public void sendReserveStockFailed(ReleaseStockCommand command){

    }

    public void sendClearCartFailed(ClearCartFailedCommand command) {

    }

    public void sendCreatePaymentFailed(CreatePaymentFailedCommand command) {

    }

    public void sendAwaitPaymentFailed(AwaitPaymentFailedCommand command) {

    }

    public void sendUpdateOrderStatusFailed(UpdateOrderStatusFailedCommand command){

    }

    public void sendCancelOrder(CancelOrderCommand command){

    }
}
