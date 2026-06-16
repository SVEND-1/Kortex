package org.example.authservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.db.UserEntity;
import org.example.authservice.db.UserRepository;
import org.example.kafkaEvent.RoleUpdateEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumer {

    private static final String TOPIC_ADMIN = "admin-service";
    private final UserRepository userRepository;

    @KafkaListener(topics = TOPIC_ADMIN,
            containerFactory = "roleUpdateKafkaListenerContainerFactory")
    public void consumeAdmin(RoleUpdateEvent event) {
        try {
            UserEntity user = userRepository.getUserEntityById(event.id());
            user.setRole(event.updatedRole());
            userRepository.save(user);
        }catch (Exception e){
            log.error("Не получилось обновить роль пользователя,ex={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
