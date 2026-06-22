package org.example.cartservice.kafka;

import lombok.RequiredArgsConstructor;
import org.example.saga.command.approve.ClearCartCommand;
import org.example.saga.event.approve.CartClearedEvent;
import org.example.saga.event.failed.CartClearedFailedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static org.example.saga.KafkaTopics.CLEAR_CART_APPROVE_EVENT;
import static org.example.saga.KafkaTopics.CLEAR_CART_FAILED_EVENT;

@RequiredArgsConstructor
@Service
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendClearCartApprove(ClearCartCommand command) {
        CartClearedEvent event = new CartClearedEvent(command.sagaId());
        kafkaTemplate.send(CLEAR_CART_APPROVE_EVENT,command.sagaId(),event);
    }

    public void sendClearCartFailed(ClearCartCommand command,String errorMessage) {
        CartClearedFailedEvent event = new CartClearedFailedEvent(command.sagaId(),
                "Не удалось очистить корзину,ex=" + errorMessage);
        kafkaTemplate.send(CLEAR_CART_FAILED_EVENT,command.sagaId(),event);
    }
}
