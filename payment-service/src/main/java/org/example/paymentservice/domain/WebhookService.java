package org.example.paymentservice.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.api.dto.response.webhook.YooKassaWebhookEvent;
import org.example.paymentservice.db.PaymentEntity;
import org.example.paymentservice.domain.http.OrderClientService;
import org.example.paymentservice.kafka.KafkaProducer;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Slf4j
@RequiredArgsConstructor
@Service
public class WebhookService {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;
    private final KafkaProducer kafkaProducer;
    private final OrderClientService orderClientService;

    public void succeededPayment(String rawBody) {
        try {
            YooKassaWebhookEvent event = objectMapper.readValue(rawBody, YooKassaWebhookEvent.class);
            String eventType = event.getEvent();
            String paymentId = event.getObject().getId();

            PaymentEntity entity = paymentService.findByPaymentId(paymentId);

            if ("payment.succeeded".equals(eventType)) {
                succeeded(event,entity);
            }
            else if ("payment.canceled".equals(eventType)) {
                canceled(event,entity);
            }
        } catch (Exception e) {
            exception(rawBody);
            log.error("Ошибка обработки вебхука,ex={}", e.getMessage());
            throw new RuntimeException("Ошибка обработки вубхука",e);
        }
    }

    private void succeeded(YooKassaWebhookEvent event,PaymentEntity entity){
        String sagaIdStr = event.getObject().getMetadata().getSagaId();
        String orderIdStr = event.getObject().getMetadata().getOrderId();

        if (orderIdStr != null && sagaIdStr != null) {
            Long orderId = Long.parseLong(orderIdStr);

            entity.setPaid(true);
            entity.setCapturedAt(LocalDateTime.now());
            paymentService.save(entity);

            orderClientService.setStatus(orderId,"PENDING",entity.getUserId());
            kafkaProducer.sendSucceededPayment(sagaIdStr);
        } else {
            log.warn("Нет orderId в metadata");
        }
    }

    private void canceled(YooKassaWebhookEvent event,PaymentEntity entity){
        String sagaIdStr = event.getObject().getMetadata().getSagaId();
        String orderIdStr =  event.getObject().getMetadata().getOrderId();
        Long orderId = Long.parseLong(orderIdStr);
        orderClientService.setStatus(orderId,"FAILED_PAYMENT",entity.getUserId());

        kafkaProducer.sendFailedPayment(sagaIdStr);
    }

    private void exception(String rawBody){
        YooKassaWebhookEvent event;
        try {
            event = objectMapper.readValue(rawBody, YooKassaWebhookEvent.class);
        }
        catch (JsonProcessingException e1) {
            log.error("Не удалось распарсить webhook");
            throw new RuntimeException("Не удалось распарсить webhook");
        }
        String sagaIdStr = event.getObject().getMetadata().getSagaId();
        kafkaProducer.sendFailedPayment(sagaIdStr);
    }
}
