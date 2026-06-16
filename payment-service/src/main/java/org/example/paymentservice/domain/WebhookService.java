package org.example.paymentservice.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.api.dto.response.webhook.YooKassaWebhookEvent;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class WebhookService {

    private final ObjectMapper objectMapper;

    public Long succeededPayment(String rawBody){
        try {
            YooKassaWebhookEvent event = objectMapper.readValue(rawBody, YooKassaWebhookEvent.class);
            String eventType = event.getEvent();
            String paymentId = event.getObject().getId();
            log.info("Событие: {}, paymentId: {}", eventType, paymentId);

            if ("payment.succeeded".equals(eventType)) {
                String orderIdStr = null;
                if (event.getObject().getMetadata() != null) {
                    orderIdStr = event.getObject().getMetadata().getOrderId();
                }
                if (orderIdStr != null) {
                    Long orderId = Long.parseLong(orderIdStr);
                    //TODO изменить статус платежа
                    return orderId;
                } else {
                    log.warn("Нет orderId в metadata");
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Ошибка обработки вебхука,ex={}", e.getMessage());
            throw new RuntimeException("Ошибка обработки вубхука",e);
        }
    }
}
