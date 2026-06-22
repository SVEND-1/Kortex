package org.example.sagaorchestrator.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.saga.OrderItem;
import org.example.saga.command.compensate.*;
import org.example.saga.event.failed.*;
import org.example.sagaorchestrator.db.*;
import org.example.sagaorchestrator.kafka.KafkaCommand;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SagaFailedManager {
    private final SagaRepository sagaRepository;
    private final KafkaCommand kafkaCommand;

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
                    break;

            }
        }

        if (saga.getOrderId() != null) {
            saga.setState(SagaState.COMPENSATING_ORDER);
            sagaRepository.save(saga);
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

}
