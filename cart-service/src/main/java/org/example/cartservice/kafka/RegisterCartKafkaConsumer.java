package org.example.cartservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cartservice.domain.CartService;
import org.example.kafkaEvent.CartRegisterEvent;
import org.example.kafkaEvent.NotifyEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RegisterCartKafkaConsumer {
    private final CartService cartService;
    private static final String TOPIC = "cart-service";


    @KafkaListener(topics = TOPIC)
    public void consumeNotify(CartRegisterEvent cartRegisterEvent) {
        cartService.create(cartRegisterEvent.userId());
    }
}
