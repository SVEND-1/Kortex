package org.example.orderservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.db.OrderStatus;
import org.example.orderservice.domain.OrderService;
import org.example.saga.command.approve.UpdateOrderStatusCommand;
import org.example.saga.command.compensate.CancelOrderCommand;
import org.example.saga.event.approve.OrderStatusUpdatedEvent;
import org.example.saga.event.failed.OrderStatusUpdatedFailedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static org.example.saga.KafkaTopics.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaConsumer {
    private final OrderService orderService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = ORDER_STATUS_COMMAND,groupId = "order-service")
    public void handlerOderStatusCommand(UpdateOrderStatusCommand command) {
        try {
            orderService.updateStatusSaga(command.orderId(), OrderStatus.PENDING);

            OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(command.sagaId());
            kafkaTemplate.send(ORDER_STATUS_APPROVE_EVENT,command.sagaId(),event);
        }catch (Exception e){
            OrderStatusUpdatedFailedEvent event = new OrderStatusUpdatedFailedEvent(command.sagaId(),
                    "Не удалось обновить статус заказа");
            kafkaTemplate.send(ORDER_STATUS_FAILED_EVENT,command.sagaId(),event);
        }
    }

    @KafkaListener(topics = ORDER_CANCEL_COMMAND)
    public void handlerOrderCancelCommand(CancelOrderCommand command) {
        try {
            orderService.deleteOrder(command.orderId());
        }catch (Exception e){
            log.error("Не удалось удалить заказ,ex={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
