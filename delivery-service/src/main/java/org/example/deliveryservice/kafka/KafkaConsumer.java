package org.example.deliveryservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.command.DeliveryCommand;
import org.example.command.DeliveryDeletedCommand;
import org.example.deliveryservice.domain.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static org.example.Topics.DELIVERY_CREATE_ORDER_COMMAND;
import static org.example.Topics.DELIVERY_DELETE_ORDER_COMMAND;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaConsumer {
    private final OrderService orderService;

    @KafkaListener(topics = DELIVERY_CREATE_ORDER_COMMAND,groupId = "delivery-service")
    public void handlerDeliveryCommand(DeliveryCommand command) {
        orderService.create(command);
    }

    @KafkaListener(topics = DELIVERY_DELETE_ORDER_COMMAND,groupId = "delivery-service")
    public void handlerDeliveryDeletedCommand(DeliveryDeletedCommand command) {
        orderService.delete(command);
    }
}
