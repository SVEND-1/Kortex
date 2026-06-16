package org.example.userservice.kafka.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kafkaEvent.RoleUpdateEvent;
import org.example.kafkaEvent.UserRegisterEvent;
import org.example.userservice.domain.UserService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserKafkaConsumer {

    private final UserService userService;
    private static final String TOPIC_USER = "user-service";
    private static final String TOPIC_ADMIN = "admin-service";

    @KafkaListener(topics = TOPIC_USER,
            containerFactory = "userRegisterKafkaListenerContainerFactory")
    public void consumeNotify(UserRegisterEvent event) {
        userService.save(event);
    }

    @KafkaListener(topics = TOPIC_ADMIN,
            containerFactory = "roleUpdateKafkaListenerContainerFactory")
    public void consumeAdmin(RoleUpdateEvent event) {
        log.info("consumeAdmin event AAAAAAAAAA: {}", event);
        userService.roleUpdate(event);
    }
}
