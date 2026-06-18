package org.example.sagaorchestrator.kafka;

import lombok.RequiredArgsConstructor;
import org.example.saga.event.StartSagaEvent;
import org.example.saga.event.approve.PaymentSuccessEvent;
import org.example.saga.event.approve.CartClearedEvent;
import org.example.saga.event.approve.OrderStatusUpdatedEvent;
import org.example.saga.event.approve.PaymentCreatedEvent;
import org.example.saga.event.approve.StockReservedEvent;
import org.example.saga.event.failed.*;
import org.example.sagaorchestrator.domain.SagaOrchestratorService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static org.example.saga.KafkaTopics.*;

@RequiredArgsConstructor
@Service
public class KafkaConsumer {
    private final SagaOrchestratorService sagaOrchestratorService;

    //Получать на каждый метод из оркестратора  вызывать

    @KafkaListener(topics = START_SAGA,groupId = "saga-orchestrator")
    public void handleStartSaga(StartSagaEvent event){
        sagaOrchestratorService.startSaga(event);
    }


    @KafkaListener(topics = PRODUCT_APPROVE_EVENT,groupId = "saga-orchestrator")
    public void handleStockReserved(StockReservedEvent event) {//
        sagaOrchestratorService.onStockReserved(event);
    }

    @KafkaListener(topics = CLEAR_CART_APPROVE_EVENT,groupId = "saga-orchestrator")
    public void handlerClearCart(CartClearedEvent event){
        sagaOrchestratorService.onClearCart(event);
    }

    @KafkaListener(topics = PAYMENT_CREATE_APPROVE_EVENT,groupId = "saga-orchestrator")
    public void handlerPaymentCreate(PaymentCreatedEvent event){
        sagaOrchestratorService.onPaymentCreated(event);
    }

    @KafkaListener(topics = PAYMENT_APPROVE_EVENT,groupId = "saga-orchestrator")
    public void handlerPayment(PaymentSuccessEvent event){
        sagaOrchestratorService.onPaymentSuccess(event);
    }

    @KafkaListener(topics = ORDER_STATUS_APPROVE_EVENT,groupId = "saga-orchestrator")
    public void handlerOrderStatus(OrderStatusUpdatedEvent event){
        sagaOrchestratorService.onUpdateOrderStatus(event);
    }

    //------------------------------------
    @KafkaListener(topics = PRODUCT_FAILED_EVENT,groupId = "saga-orchestrator")
    public void handleStockReservationFailed(StockReservationFailedEvent event){
        sagaOrchestratorService.onStockReservationFailed(event);
    }

    @KafkaListener(topics = CLEAR_CART_FAILED_EVENT,groupId = "saga-orchestrator")
    public void handlerClearCartFailed(CartClearedFailedEvent event){
        sagaOrchestratorService.onClearCartFailed(event);
    }

    @KafkaListener(topics = PAYMENT_CREATE_FAILED_EVENT,groupId = "saga-orchestrator")
    public void handlerPaymentCreateFailed(PaymentCreatedFailedEvent event){
        sagaOrchestratorService.onPaymentCreatedFailed(event);
    }

    @KafkaListener(topics = PAYMENT_FAILED_EVENT,groupId = "saga-orchestrator")
    public void handlerPaymentFailed(PaymentFailedEvent event){
        sagaOrchestratorService.onPaymentFailed(event);
    }

    @KafkaListener(topics = ORDER_STATUS_FAILED_EVENT,groupId = "saga-orchestrator")
    public void handlerOrderStatusFailed(OrderStatusUpdatedFailedEvent event){
        sagaOrchestratorService.onUpdateOrderStatusFailed(event);
    }

}
