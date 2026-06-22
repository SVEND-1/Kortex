package org.example.orderservice.kafka;

import lombok.RequiredArgsConstructor;
import org.example.command.DeliveryDeletedCommand;
import org.example.saga.command.approve.UpdateOrderStatusCommand;
import org.example.saga.event.StartSagaEvent;
import org.example.saga.event.approve.OrderStatusUpdatedEvent;
import org.example.saga.event.failed.OrderStatusUpdatedFailedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static org.example.Topics.DELIVERY_DELETE_ORDER_COMMAND;
import static org.example.saga.KafkaTopics.*;

@RequiredArgsConstructor
@Service
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderUpdateStatusApprove(UpdateOrderStatusCommand command){
        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(command.sagaId());
        kafkaTemplate.send(ORDER_STATUS_APPROVE_EVENT,command.sagaId(),event);
    }

    public void sendOrderUpdateStatusFailed(UpdateOrderStatusCommand command,String errorMessage){
        OrderStatusUpdatedFailedEvent event = new OrderStatusUpdatedFailedEvent(command.sagaId(),
                "Не удалось обновить статус заказа,ex=" + errorMessage);
        kafkaTemplate.send(ORDER_STATUS_FAILED_EVENT,command.sagaId(),event);
    }

    public void sendStartSaga(StartSagaEvent event) {
        kafkaTemplate.send(START_SAGA, event);
    }

    public void sendDeliveryDeleted(DeliveryDeletedCommand command) {
        kafkaTemplate.send(DELIVERY_DELETE_ORDER_COMMAND, String.valueOf(command.orderId()),command);
    }
}
