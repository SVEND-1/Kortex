package org.example.paymentservice.domain;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ApiException;
import org.example.paymentservice.domain.mapper.ReceiptMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.loolzaaa.youkassa.model.Payment;
import ru.loolzaaa.youkassa.pojo.*;
import ru.loolzaaa.youkassa.processors.PaymentProcessor;

import java.util.Map;

@Slf4j
@Component
public class PaymentYooKassaService {

    @Value("${app.base-url:http://localhost:8080/payments}")
    private String RETURN_URL;

    public Payment findPayment(PaymentProcessor paymentProcessor, String paymentId) {
        try {
            return paymentProcessor.findById(paymentId);
        } catch (ApiException e) {
            log.error("Ошибка поиска платежа {}: {}", paymentId, e.getMessage());
            throw new RuntimeException("Платеж не найден", e);
        }
    }

    public Payment createYooKassaPayment(PaymentProcessor paymentProcessor, String idempotencyKey,String summa,Long orderId,String sagaId) {
        try {
            Amount amount = Amount.builder()
                    .value(summa)
                    .currency(Currency.RUB)
                    .build();

            Confirmation confirmation = Confirmation.builder()
                    .type(Confirmation.Type.REDIRECT)
                    .returnUrl(RETURN_URL)
                    .build();

            Map<String, String> metadata = Map.of(
                    "orderId", orderId.toString(),
                    "sagaId", sagaId
            );

            Payment payment = Payment.builder()
                    .amount(amount)
                    .description("Оплата заказа в Kortex")
                    .confirmation(confirmation)
                    .metadata(metadata)
                    .capture(true)
                    .build();

            return paymentProcessor.create(payment, idempotencyKey);
        } catch (Exception e) {
            log.error("Не удалось создать платеж yookassa,ex={}", e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}

