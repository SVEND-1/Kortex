package org.example.sagaorchestrator.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.saga.OrderItem;
import org.example.saga.event.StartSagaEvent;
import org.example.saga.event.approve.*;
import org.example.saga.event.failed.*;
import org.example.sagaorchestrator.db.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class SagaOrchestratorService {

    private final SagaRepository sagaRepository;
    private final SagaApproveManager sagaApproveManager;
    private final SagaFailedManager sagaFailedManager;

    public void startSaga(StartSagaEvent event) {
        String sagaId = UUID.randomUUID().toString();
        SagaEntity saga = SagaEntity.builder()
                .id(sagaId)
                .userId(event.userId())
                .orderId(event.orderId())
                .items(convertEntityToOrderItems(event.orderItems()))
                .totalAmount(event.totalAmount())
                .state(SagaState.STARTED)
                .executedSteps(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();
        sagaRepository.save(saga);

        onStockReserved(saga);
    }

    //----------------------------APPROVE----------------------------------------

    private void onStockReserved(SagaEntity saga) {
        sagaApproveManager.onStockReserved(saga);
    }

    public void onStockReserved(StockReservedEvent event) {
        sagaApproveManager.onStockReserved(event);
    }

    public void onClearCart(CartClearedEvent event){
        sagaApproveManager.onClearCart(event);
    }

    public void onPaymentCreated(PaymentCreatedEvent event) {
        sagaApproveManager.onPaymentCreated(event);
    }

    public void onPaymentSuccess(PaymentSuccessEvent event){
        sagaApproveManager.onPaymentSuccess(event);
    }

    public void onUpdateOrderStatus(OrderStatusUpdatedEvent event){
        sagaApproveManager.onUpdateOrderStatus(event);
    }

    //---------------------------FAILED---------------------------------------

    public void onStockReservationFailed(StockReservationFailedEvent event) {
        sagaFailedManager.onStockReservationFailed(event);
    }


    public void onClearCartFailed(CartClearedFailedEvent event){
        sagaFailedManager.onClearCartFailed(event);
    }


    public void onPaymentCreatedFailed(PaymentCreatedFailedEvent event){
        sagaFailedManager.onPaymentCreatedFailed(event);
    }


    public void onPaymentFailed(PaymentFailedEvent event){
        sagaFailedManager.onPaymentFailed(event);
    }


    public void onUpdateOrderStatusFailed(OrderStatusUpdatedFailedEvent event){
        sagaFailedManager.onUpdateOrderStatusFailed(event);
    }

    private List<OrderItemEntity> convertEntityToOrderItems(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(el -> new OrderItemEntity(el.productId(),el.quantity()))
                .toList();
    }

}
