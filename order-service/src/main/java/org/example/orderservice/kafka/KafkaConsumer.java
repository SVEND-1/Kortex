package org.example.orderservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.db.OrderStatus;
import org.example.orderservice.domain.OrderService;
import org.example.saga.command.approve.UpdateOrderStatusCommand;
import org.example.saga.command.compensate.CancelOrderCommand;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static org.example.saga.KafkaTopics.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaConsumer {
    private final OrderService orderService;
    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = ORDER_STATUS_COMMAND,groupId = "order-service")
    public void handlerOderStatusCommand(UpdateOrderStatusCommand command) {
        try {
            orderService.updateStatusSaga(command.orderId(), OrderStatus.PENDING);
            kafkaProducer.sendOrderUpdateStatusApprove(command);
        }catch (Exception e){
            kafkaProducer.sendOrderUpdateStatusFailed(command,e.getMessage());
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
