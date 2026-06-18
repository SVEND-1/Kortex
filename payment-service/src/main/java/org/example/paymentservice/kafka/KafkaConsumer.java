package org.example.paymentservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.api.dto.response.payment.PaymentCreateResponse;
import org.example.paymentservice.domain.OrderClientService;
import org.example.paymentservice.domain.PaymentService;
import org.example.saga.command.approve.CreatePaymentCommand;
import org.example.saga.command.compensate.CreatePaymentFailedCommand;
import org.example.saga.event.approve.PaymentCreatedEvent;
import org.example.saga.event.failed.PaymentCreatedFailedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static org.example.saga.KafkaTopics.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PaymentService paymentService;
    private final OrderClientService orderClientService;

    @KafkaListener(topics = PAYMENT_CREATE_COMMAND,groupId = "payment-service")
    public void handlePaymentCreateCommand(CreatePaymentCommand command) {
        try {
            PaymentCreateResponse response = paymentService.createPayment(command.amount(),command.orderId(),command.userID(),command.sagaId());
            //ТУТ БЫ ЕЩЁ КАК ТО МЕНЯТЬ СТАТУС И ПРОВЕРКА ЧЕРЕЗ ВЕБ СОКЕТ ЧТОБЫ ПЕРЕКИНУТЬ ПОЛЬЗОВАТЕЛЯ НА ДРУГУЮ СТРАНИЦУ
            orderClientService.setStatus(command.orderId(),"AWAITING_PAYMENT", command.userID());
            PaymentCreatedEvent event = new PaymentCreatedEvent(command.sagaId(), response.paymentId());
            kafkaTemplate.send(PAYMENT_CREATE_APPROVE_EVENT,command.sagaId(),event);
        }catch (Exception e) {
            log.error("Не удалось создать платеж,ex={}", e.getMessage());

            PaymentCreatedFailedEvent event = new PaymentCreatedFailedEvent(command.sagaId(),
                    "Не удалось создать платеж,ex=" + e.toString());
            kafkaTemplate.send(PAYMENT_CREATE_FAILED_EVENT,command.sagaId(),event);
        }
    }

    @KafkaListener(topics = PAYMENT_CREATE_COMPENSATE_COMMAND,groupId = "payment-service")
    public void handlePaymentCreateCompensateCommand(CreatePaymentFailedCommand command) {
        try {
            //TODO удаление платежа
        }catch (Exception e) {
            log.error("Не удалось удалить платеж,ex={}", e.getMessage());
        }
    }
}
