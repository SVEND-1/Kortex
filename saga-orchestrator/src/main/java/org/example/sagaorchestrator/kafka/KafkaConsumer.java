package org.example.sagaorchestrator.kafka;

import lombok.RequiredArgsConstructor;
import org.example.saga.event.approve.StockReservedEvent;
import org.example.saga.event.failed.StockReservationFailedEvent;
import org.example.sagaorchestrator.domain.SagaOrchestratorService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static org.example.saga.KafkaTopics.PRODUCT_APPROVE_EVENT;
import static org.example.saga.KafkaTopics.PRODUCT_FAILED_EVENT;

@RequiredArgsConstructor
@Service
public class KafkaConsumer {
    private final SagaOrchestratorService sagaOrchestratorService;

    //Получать на каждый метод из оркестратора  вызывать

    @KafkaListener(topics = PRODUCT_APPROVE_EVENT,groupId = "saga-orchestrator")
    public void handleStockReserved(StockReservedEvent event) {//
        sagaOrchestratorService.onStockReserved(event);
    }

    @KafkaListener(topics = PRODUCT_FAILED_EVENT,groupId = "saga-orchestrator")
    public void handleStockReservationFailed(StockReservationFailedEvent event){
        sagaOrchestratorService.onStockReservationFailed(event);
    }
}
