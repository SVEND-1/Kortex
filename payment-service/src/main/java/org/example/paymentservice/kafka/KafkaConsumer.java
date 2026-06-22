package org.example.paymentservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.api.dto.response.payment.PaymentCreateResponse;
import org.example.paymentservice.domain.http.OrderClientService;
import org.example.paymentservice.domain.PaymentService;
import org.example.saga.command.approve.CreatePaymentCommand;
import org.example.saga.command.compensate.CreatePaymentFailedCommand;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static org.example.saga.KafkaTopics.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaConsumer {

    private final PaymentService paymentService;
    private final OrderClientService orderClientService;
    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = PAYMENT_CREATE_COMMAND,groupId = "payment-service")
    public void handlePaymentCreateCommand(CreatePaymentCommand command) {
        try {
            PaymentCreateResponse response = paymentService.createPayment(command.orderId(),command.userID(),command.sagaId());

            kafkaProducer.sendPaymentCreateApprove(command, response.paymentId());
            orderClientService.setStatus(command.orderId(),"AWAITING_PAYMENT", command.userID());
        }catch (Exception e) {
            log.error("Не удалось создать платеж,ex={}", e.getMessage());
            kafkaProducer.sendPaymentCreateFailed(command, e.getMessage());
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
