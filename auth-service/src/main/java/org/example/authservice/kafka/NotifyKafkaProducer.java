package org.example.authservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.example.kafkaEvent.CartRegisterEvent;
import org.example.kafkaEvent.NotifyEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotifyKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_AUTH = "notification-service";
    private static final String TOPIC_CART = "cart-service";

    public NotifyKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessageToKafka(NotifyEvent event) {
        try {
            kafkaTemplate.send(TOPIC_AUTH, event);
        } catch (Exception e) {
            log.error("Ошибка отправки в Kafka auth: {}", e.getMessage());
        }
    }

    public void sendMessageToKafkaCart(CartRegisterEvent event) {
        try {
            kafkaTemplate.send(TOPIC_CART, event);
        } catch (Exception e) {
            log.error("Ошибка отправки в Kafka cart: {}", e.getMessage());
        }
    }
}