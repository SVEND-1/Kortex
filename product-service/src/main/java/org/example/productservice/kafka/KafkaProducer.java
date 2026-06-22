package org.example.productservice.kafka;

import lombok.RequiredArgsConstructor;
import org.example.saga.command.approve.ReserveStockCommand;
import org.example.saga.event.approve.StockReservedEvent;
import org.example.saga.event.failed.StockReservationFailedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static org.example.saga.KafkaTopics.PRODUCT_APPROVE_EVENT;
import static org.example.saga.KafkaTopics.PRODUCT_FAILED_EVENT;

@RequiredArgsConstructor
@Service
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendProductApprove(ReserveStockCommand command){
        StockReservedEvent event = new StockReservedEvent(command.sagaId());
        kafkaTemplate.send(PRODUCT_APPROVE_EVENT,command.sagaId(),event);
    }

    public void sendProductFailed(ReserveStockCommand command,String errorMessage){
        StockReservationFailedEvent event = new StockReservationFailedEvent(command.sagaId(),
                "Не удалось зарезервировать товар,ex=" + errorMessage);
        kafkaTemplate.send(PRODUCT_FAILED_EVENT,command.sagaId(),event);
    }
}
