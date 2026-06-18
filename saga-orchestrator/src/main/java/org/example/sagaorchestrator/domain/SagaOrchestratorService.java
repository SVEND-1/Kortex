package org.example.sagaorchestrator.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.saga.OrderItem;
import org.example.saga.command.approve.*;
import org.example.saga.command.compensate.*;
import org.example.saga.event.StartSagaEvent;
import org.example.saga.event.approve.*;
import org.example.saga.event.failed.*;
import org.example.sagaorchestrator.db.*;
import org.example.sagaorchestrator.kafka.KafkaCommand;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class SagaOrchestratorService {

    private final SagaRepository sagaRepository;
    private final KafkaCommand kafkaCommand;

    //Зарезервировать товар
    //Очистить корзину от этих товаров
    //Создать платеж
    //Платеж прошел
    //Сменить статус заказа

    //В команды добавлять что надо для работы и + sagaId
    //В ивентах возвращать sagaId + тру или фолс
    public String  startSaga(StartSagaEvent event) {
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
        return sagaId;
    }

    private void onStockReserved(SagaEntity saga) {//ЗАПУСКАЕТ ЦЕПОЧКУ
        saga.setState(SagaState.RESERVE_STOCK);
        sagaRepository.save(saga);
        log.info("Отправка резервации товара в кафку,sagaId={}", saga.getId());

        kafkaCommand.sendReserveStock(new ReserveStockCommand(saga.getId(),convertOrderItems(saga)));
    }


    public void onStockReserved(StockReservedEvent event) {//ВЫЗЫВАЕТСЯ В CONSUMER
        SagaEntity saga = loadSaga(event.sagaId());
        if (isStepMismatch(saga, SagaState.RESERVE_STOCK)) return;

        log.info("Товар зарезервирован для saga={}", event.sagaId());

        saga.getExecutedSteps().add(SagaStep.RESERVE_STOCK);
        saga.setState(SagaState.CLEAR_CART);
        sagaRepository.save(saga);

        kafkaCommand.sendClearCart(new ClearCartCommand(saga.getId(),saga.getUserId(),convertOrderItems(saga)));
    }


    public void onClearCart(CartClearedEvent event){
        SagaEntity saga = loadSaga(event.sagaId());
        if(isStepMismatch(saga, SagaState.CLEAR_CART)) return;

        log.info("Коризна очищина для saga={}", event.sagaId());

        saga.getExecutedSteps().add(SagaStep.CLEAR_CART);
        saga.setState(SagaState.CREATE_PAYMENT);
        sagaRepository.save(saga);

        kafkaCommand.sendCreatePayment(new CreatePaymentCommand(saga.getId(),saga.getOrderId(),saga.getUserId(),saga.getTotalAmount()));
    }

    public void onPaymentCreated(PaymentCreatedEvent event) {
        SagaEntity saga = loadSaga(event.sagaId());
        if(isStepMismatch(saga, SagaState.CREATE_PAYMENT)) return;

        log.info("Платеж создался для saga={}", saga.getId());

        saga.getExecutedSteps().add(SagaStep.CREATE_PAYMENT);
        saga.setState(SagaState.AWAIT_PAYMENT);
        saga.setPaymentId(event.paymentId());
        sagaRepository.save(saga);
    }

    public void onPaymentSuccess(PaymentSuccessEvent event){//ПЕРЕМЕНОВАТЬ НАДО ВЕЗДЕ БЕЗ AWAIT_PAYMENT
        SagaEntity saga = loadSaga(event.sagaId());
        if(isStepMismatch(saga, SagaState.AWAIT_PAYMENT)) return;

        saga.getExecutedSteps().add(SagaStep.PAYMENT_SUCCESS);
        saga.setState(SagaState.UPDATE_ORDER_STATUS);
        sagaRepository.save(saga);

        kafkaCommand.sendUpdateOrderStatus(new UpdateOrderStatusCommand(saga.getId(),saga.getOrderId()));
    }


    public void onUpdateOrderStatus(OrderStatusUpdatedEvent event){
        SagaEntity saga = loadSaga(event.sagaId());
        if(isStepMismatch(saga, SagaState.UPDATE_ORDER_STATUS)) return;

        log.info("Статус обновлен для saga={}", saga.getId());

        saga.getExecutedSteps().add(SagaStep.UPDATE_ORDER_STATUS);
        saga.setState(SagaState.COMPLETED);
        sagaRepository.save(saga);
    }



    public void onStockReservationFailed(StockReservationFailedEvent event) {
        SagaEntity saga = loadSaga(event.sagaId());
        log.error("saga={} Ошибка резервирования: {}", saga.getId(), event.reason());
        saga.setErrorMessage(event.reason());
        sagaRepository.save(saga);

        compensate(saga);
    }


    public void onClearCartFailed(CartClearedFailedEvent event){
        SagaEntity saga = loadSaga(event.sagaId());
        log.error("saga={} ошибка очистки корзины: {}", saga.getId(), event.reason());
        saga.setErrorMessage(event.reason());
        sagaRepository.save(saga);

        compensate(saga);
    }


    public void onPaymentCreatedFailed(PaymentCreatedFailedEvent event){
        SagaEntity saga = loadSaga(event.sagaId());
        log.error("saga={} ошибка создания платежа: {}", saga.getId(), event.reason());
        saga.setErrorMessage(event.reason());
        sagaRepository.save(saga);

        compensate(saga);
    }


    public void onPaymentFailed(PaymentFailedEvent event){
        SagaEntity saga = loadSaga(event.sagaId());
        log.error("saga={} платеж не прошел: {}", saga.getId(), event.reason());
        saga.setErrorMessage(event.reason());
        sagaRepository.save(saga);

        compensate(saga);
    }


    public void onUpdateOrderStatusFailed(OrderStatusUpdatedFailedEvent event){
        SagaEntity saga = loadSaga(event.sagaId());
        log.error("saga={} ошибка обновление статуса платежа: {}", saga.getId(), event.reason());
        saga.setErrorMessage(event.reason());
        sagaRepository.save(saga);

        compensate(saga);
    }


    private void compensate(SagaEntity saga) {
        List<SagaStep> steps = new ArrayList<>(saga.getExecutedSteps());
        Collections.reverse(steps);
        for (SagaStep step : steps) {
            switch (step) {
                case SagaStep.RESERVE_STOCK:
                    saga.setState(SagaState.COMPENSATING_STOCK);
                    sagaRepository.save(saga);
                    log.info("saga={} Компенсация: освобождение резерва", saga.getId());
                    kafkaCommand.sendReserveStockFailed(
                            new ReleaseStockCommand(saga.getId(),convertOrderItems(saga))
                    );
                    break;
                case SagaStep.CLEAR_CART:
                    saga.setState(SagaState.COMPENSATING_CART);
                    sagaRepository.save(saga);
                    log.info("saga={} Компенсация: восстановление корзины", saga.getId());
                    kafkaCommand.sendClearCartFailed(
                            new ClearCartFailedCommand(saga.getId(),saga.getUserId(),convertOrderItems(saga))
                    );
                    break;
                case SagaStep.CREATE_PAYMENT:
                    if(saga.getPaymentId() != null) {//Если вернули уже деньги не надо удалять
                        saga.setState(SagaState.COMPENSATING_PAYMENT);
                        sagaRepository.save(saga);
                        log.info("saga={} Компенсация: отмена платежа (не оплачен)", saga.getId());
                        kafkaCommand.sendCreatePaymentFailed(
                                new CreatePaymentFailedCommand(saga.getId(), saga.getPaymentId())
                        );
                    }
                    break;
                case SagaStep.PAYMENT_SUCCESS:
                    saga.setState(SagaState.COMPENSATING_PAYMENT);
                    sagaRepository.save(saga);
                    log.info("saga={} Компенсация: возврат платежа ", saga.getId());
                    kafkaCommand.sendPaymentFailed(
                            new PaymentFailedCommand(saga.getId(),saga.getPaymentId())//Вернуть деньги
                    );
                    break;
                case SagaStep.UPDATE_ORDER_STATUS:
                    //Тут не может по идее ничего пойти не так
                    break;

            }
        }

        if (saga.getOrderId() != null) {//даже если просто резервация не прошла надо уже удалять заказ
            saga.setState(SagaState.COMPENSATING_ORDER);
            sagaRepository.save(saga);
            log.info("saga={} Компенсация: отмена заказа orderId={}", saga.getId(), saga.getOrderId());
            kafkaCommand.sendCancelOrder(new CancelOrderCommand(saga.getId(), saga.getOrderId()));
        }
        failSaga(saga,saga.getErrorMessage());
    }

    private void failSaga(SagaEntity saga, String reason) {
        log.error("Saga: {}  провалена. Причина: {}", saga.getId(), reason);
        saga.setState(SagaState.FAILED);
        saga.setErrorMessage(reason);
        sagaRepository.save(saga);
    }


    private SagaEntity loadSaga(String sagaId) {
        return sagaRepository.findById(sagaId)
                .orElseThrow(() -> new EntityNotFoundException("Сага не найдена,id=" + sagaId));
    }

    private List<OrderItem> convertOrderItems(SagaEntity saga) {
        return saga.getItems().stream()
                .map(el -> new OrderItem(el.getProductId(), el.getQuantity()))
                .toList();
    }

    private List<OrderItemEntity> convertEntityToOrderItems(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(el -> new OrderItemEntity(el.productId(),el.quantity()))
                .toList();
    }

    private boolean isStepMismatch(SagaEntity saga, SagaState expected) {
        if (saga.getState() != expected) {
            log.warn("Saga={} Несоответствие шага: ожидался {} а фактически {} — пропускаем дублирующее событие",
                    saga.getId(), expected, saga.getState());
            return true;
        }
        return false;
    }


}
