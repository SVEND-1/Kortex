package org.example.deliveryservice.kafka;


import lombok.RequiredArgsConstructor;
import org.example.command.ItemsDelivery;
import org.example.kafkaEvent.ProductReturnEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static org.example.Topics.PRODUCT_RETURN_EVENT;

@RequiredArgsConstructor
@Service
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendProductReturn(ProductReturnEvent event) {
        kafkaTemplate.send(PRODUCT_RETURN_EVENT,event);
    }
}
