package org.example.paymentservice.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.api.dto.response.webhook.YooKassaWebhookEvent;
import org.example.paymentservice.db.PaymentEntity;
import org.example.saga.event.approve.PaymentSuccessEvent;
import org.example.saga.event.failed.PaymentFailedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static org.example.saga.KafkaTopics.PAYMENT_APPROVE_EVENT;
import static org.example.saga.KafkaTopics.PAYMENT_FAILED_EVENT;

@Slf4j
@RequiredArgsConstructor
@Service
public class WebhookService {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderClientService orderClientService;

    public void succeededPayment(String rawBody) {
        try {
            YooKassaWebhookEvent event = objectMapper.readValue(rawBody, YooKassaWebhookEvent.class);
            String eventType = event.getEvent();
            String paymentId = event.getObject().getId();
            PaymentEntity entity = paymentService.findByPaymentId(paymentId);
            String sagaIdStr = null;
            String orderIdStr = null;
            log.info("Событие: {}, paymentId: {}", eventType, paymentId);

            if ("payment.succeeded".equals(eventType)) {

                if (event.getObject().getMetadata() != null ) {
                    orderIdStr = event.getObject().getMetadata().getOrderId();
                    sagaIdStr = event.getObject().getMetadata().getSagaId();
                }
                if (orderIdStr != null && sagaIdStr != null) {
                    Long orderId = Long.parseLong(orderIdStr);
                    orderClientService.setStatus(orderId,"PENDING",entity.getUserId());
                    entity.setPaid(true);
                    entity.setCapturedAt(LocalDateTime.now());
                    paymentService.save(entity);

                    PaymentSuccessEvent successEvent = new PaymentSuccessEvent(sagaIdStr);
                    kafkaTemplate.send(PAYMENT_APPROVE_EVENT,sagaIdStr, successEvent);
                } else {
                    log.warn("Нет orderId в metadata");
                }
            }
            else if ("payment.canceled".equals(eventType)) {
                orderIdStr =  event.getObject().getMetadata().getOrderId();
                Long orderId = Long.parseLong(orderIdStr);
                orderClientService.setStatus(orderId,"FAILED_PAYMENT",entity.getUserId());

                failedPayment(sagaIdStr);
            }
        } catch (Exception e) {
            YooKassaWebhookEvent event;
            try {
                event = objectMapper.readValue(rawBody, YooKassaWebhookEvent.class);
            }
            catch (JsonProcessingException e1) {
                log.error("Не удалось распарсить webhook");
                throw new RuntimeException("Не удалось распарсить webhook");
            }
            String sagaIdStr = event.getObject().getMetadata().getSagaId();
            failedPayment(sagaIdStr);

            log.error("Ошибка обработки вебхука,ex={}", e.getMessage());
            throw new RuntimeException("Ошибка обработки вубхука",e);
        }
    }

    private void failedPayment(String sagaId){
        PaymentFailedEvent event = new PaymentFailedEvent(sagaId,"Не удалось проверсти успешную оплату");
        kafkaTemplate.send(PAYMENT_FAILED_EVENT,event.sagaId(),event);
    }
}
