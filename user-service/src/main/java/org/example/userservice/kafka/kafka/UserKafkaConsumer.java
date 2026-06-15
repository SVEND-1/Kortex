package org.example.userservice.kafka.kafka;

import lombok.RequiredArgsConstructor;
import org.example.kafkaEvent.UserRegisterEvent;
import org.example.userservice.domain.UserService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserKafkaConsumer {

    private final UserService userService;
    private final String topic = "user-service";

    @KafkaListener(topics = topic)
    public void consumeNotify(UserRegisterEvent event) {
        userService.save(event);
    }
}
