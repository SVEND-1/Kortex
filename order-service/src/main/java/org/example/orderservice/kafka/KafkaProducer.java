package org.example.orderservice.kafka;

import lombok.RequiredArgsConstructor;
import org.example.saga.event.StartSagaEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static org.example.saga.KafkaTopics.START_SAGA;

@RequiredArgsConstructor
@Service
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendStartSaga(StartSagaEvent event) {
        kafkaTemplate.send(START_SAGA, event);
    }
}
