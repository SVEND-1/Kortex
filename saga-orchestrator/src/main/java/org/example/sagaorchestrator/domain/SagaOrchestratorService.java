package org.example.sagaorchestrator.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sagaorchestrator.db.SagaEntity;
import org.example.sagaorchestrator.db.SagaRepository;
import org.example.sagaorchestrator.db.SagaState;
import org.example.sagaorchestrator.dto.OrderCreateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    //Зарезервировать товар
    //Очистить корзину от этих товаров
    //Создать платеж
    //Платеж прошел
    //Сменить статус заказа

    //В команды добавлять что надо для работы и + sagaId
    //В ивентах возвращать sagaId + тру или фолс
    public String  startSaga(OrderCreateRequest request) {
        String sagaId = UUID.randomUUID().toString();
        SagaEntity saga = SagaEntity.builder()
                .id(sagaId)
                .userId(request.userId)
                .orderId(request.orderId)
                .items(request.items)
                .totalAmount(request.totalAmount)
                .state(SagaState.STARTED)
                .executedSteps(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();
        sagaRepository.save(saga);

        onStockReserved(request.items);
        return sagaId;
    }

    public void onStockReserved(SagaEntity saga) {//ЗАПУСКАЕТ ЦЕПОЧКУ
        saga.setState(SagaState.RESERVE_STOCK);
        sagaRepository.save(saga);
        log.info("Отправка резервации товара в кафку,sagaId={}", saga.getId());

        publisher.sendReserveStock(Commands.ReserveStockCommand.builder()/
                .sagaId(saga.getId())
                .orderId(saga.getOrderId())
                .items(saga.getItems())
                .build())
    }

    @Transactional
    public void onStockReserved(Events.StockReservedEvent event) {//ВЫЗЫВАЕТСЯ В CONSUMER
        SagaEntity saga = loadSaga(event.getSagaId());
        if (isStepMismatch(saga, SagaState.RESERVE_STOCK)) return;

        log.info("Товар зарезервирован для orderId={}", event.getSagaId(), event.getOrderId());

        // Добавляем шаг в историю и переключаем состояние
        saga.getExecutedSteps().add("RESERVE_STOCK");
        saga.setState(SagaState.CLEAR_CART);
        sagaRepository.save(saga);

        // Очистка корзины
        publisher.sendClearCart(Commands.ClearCartCommand.builder()//ОТПРАВКА В СЛЕДУЮЩИЮ
                .sagaId(saga.getId())
                .userId(saga.getUserId())
                .items(saga.getItems())
                .build());
    }

    @Transactional
    public void onStockReservationFailed(Events.StockReservationFailedEvent event) {//TODO компенсирующая удалить надо orderId
        SagaEntity saga = loadSaga(event.getSagaId());
        log.error("[SAGA:{}] ✘ Ошибка резервирования: {}", saga.getId(), event.getReason());
        saga.setErrorMessage(event.getReason());
        sagaRepository.save(saga);
        // Резерв не был сделан → просто отменяем заказ
        compensate(saga);
    }

    //TODO и тд

    private void compensate(SagaEntity saga) {
        List<String> steps = new ArrayList<>(saga.getExecutedSteps());
        Collections.reverse(steps);
        for (String step : steps) {
            switch (step) {
                //TODO пройтись по всем и отправь компенсирующие
            }
        }
        failSaga(saga,saga.getErrorMessage());
    }

    private void failSaga(SagaEntity saga, String reason) {
        log.error("[SAGA:{}] 💀 Сага ПРОВАЛЕНА. Причина: {}", saga.getId(), reason);
        saga.setState(SagaState.FAILED);
        saga.setErrorMessage(reason);
        sagaRepository.save(saga);
    }


    private SagaEntity loadSaga(String sagaId) {
        return sagaRepository.findById(sagaId)
                .orElseThrow(() -> new EntityNotFoundException("Сага не найдена,id=" + sagaId));
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
