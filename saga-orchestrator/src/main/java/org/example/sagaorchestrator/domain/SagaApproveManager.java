package org.example.sagaorchestrator.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.saga.OrderItem;
import org.example.saga.command.approve.ClearCartCommand;
import org.example.saga.command.approve.CreatePaymentCommand;
import org.example.saga.command.approve.ReserveStockCommand;
import org.example.saga.command.approve.UpdateOrderStatusCommand;
import org.example.saga.event.approve.*;
import org.example.sagaorchestrator.db.*;
import org.example.sagaorchestrator.kafka.KafkaCommand;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SagaApproveManager {

    private final SagaRepository sagaRepository;
    private final KafkaCommand kafkaCommand;

    public void onStockReserved(SagaEntity saga) {
        saga.setState(SagaState.RESERVE_STOCK);
        sagaRepository.save(saga);
        log.info("Отправка резервации товара в кафку,sagaId={}", saga.getId());

        kafkaCommand.sendReserveStock(new ReserveStockCommand(saga.getId(),convertOrderItems(saga)));
    }

    public void onStockReserved(StockReservedEvent event) {
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

        log.info("Корзина очищена для saga={}", event.sagaId());

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

    private SagaEntity loadSaga(String sagaId) {
        return sagaRepository.findById(sagaId)
                .orElseThrow(() -> new EntityNotFoundException("Сага не найдена,id=" + sagaId));
    }

    private List<OrderItem> convertOrderItems(SagaEntity saga) {
        return saga.getItems().stream()
                .map(el -> new OrderItem(el.getProductId(), el.getQuantity()))
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
