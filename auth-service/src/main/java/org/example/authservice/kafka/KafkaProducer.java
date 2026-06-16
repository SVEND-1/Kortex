package org.example.authservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kafkaEvent.CartRegisterEvent;
import org.example.kafkaEvent.NotifyEvent;
import org.example.kafkaEvent.UserRegisterEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_AUTH = "notification-service";
    private static final String TOPIC_CART = "cart-service";
    private static final String TOPIC_USER = "user-service";

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

    public void sendMessageToKafkaUser(UserRegisterEvent event) {
        try {
            kafkaTemplate.send(TOPIC_USER, event);
        }catch (Exception e){
            log.error("Ошибка отправки в Kafka user: {}", e.getMessage());
        }
    }
}