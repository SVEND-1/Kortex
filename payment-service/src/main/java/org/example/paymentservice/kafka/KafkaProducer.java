package org.example.paymentservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.saga.command.approve.CreatePaymentCommand;
import org.example.saga.event.approve.PaymentCreatedEvent;
import org.example.saga.event.approve.PaymentSuccessEvent;
import org.example.saga.event.failed.PaymentCreatedFailedEvent;
import org.example.saga.event.failed.PaymentFailedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static org.example.saga.KafkaTopics.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentCreateApprove(CreatePaymentCommand command,String paymentId) {
        PaymentCreatedEvent event = new PaymentCreatedEvent(command.sagaId(), paymentId);
        kafkaTemplate.send(PAYMENT_CREATE_APPROVE_EVENT,command.sagaId(),event);
    }

    public void sendPaymentCreateFailed(CreatePaymentCommand command,String errorMessage){
        PaymentCreatedFailedEvent event = new PaymentCreatedFailedEvent(command.sagaId(),
                "Не удалось создать платеж,ex=" + errorMessage);
        kafkaTemplate.send(PAYMENT_CREATE_FAILED_EVENT,command.sagaId(),event);
    }

    public void sendSucceededPayment(String sagaIdStr) {
        PaymentSuccessEvent successEvent = new PaymentSuccessEvent(sagaIdStr);
        kafkaTemplate.send(PAYMENT_APPROVE_EVENT,sagaIdStr, successEvent);
    }

    public void sendFailedPayment(String sagaIdStr) {
        PaymentFailedEvent event = new PaymentFailedEvent(sagaIdStr,"Не удалось проверсти успешную оплату");
        kafkaTemplate.send(PAYMENT_FAILED_EVENT,event.sagaId(),event);
    }

}
