package org.example.paymentservice.domain;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.loolzaaa.youkassa.client.ApiClient;
import ru.loolzaaa.youkassa.client.ApiClientBuilder;
import ru.loolzaaa.youkassa.model.Payment;
import ru.loolzaaa.youkassa.pojo.*;
import ru.loolzaaa.youkassa.processors.PaymentProcessor;

import java.util.Map;

@Slf4j
@Component
public class PaymentYooKassaService {

    @Value("${app.base-url:http://localhost:8080/payments}")
    private String RETURN_URL;

    @Value("${shop_id}")
    private String shopId;

    @Value("${payment_key}")
    private String secretKey;

    private ApiClient apiClient;
    private PaymentProcessor paymentProcessor;

    @PostConstruct
    public void init() {
        apiClient = ApiClientBuilder.newBuilder()
                .configureBasicAuth(shopId, secretKey)
                .build();
        paymentProcessor = new PaymentProcessor(apiClient);

        log.info("YooKassa инициализирована");
    }

    public Payment createYooKassaPayment(String idempotencyKey,String summa,Long orderId,String sagaId) {
        try {
            Amount amount = buildAmount(summa);
            Confirmation confirmation = buildConfirmation();
            Map<String, String> metadata = buildMetadata(orderId, sagaId);

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

    private Amount buildAmount(String summa) {
        return Amount.builder()
                .value(summa)
                .currency(Currency.RUB)
                .build();
    }

    private Confirmation buildConfirmation() {
        return Confirmation.builder()
                .type(Confirmation.Type.REDIRECT)
                .returnUrl(RETURN_URL)
                .build();
    }

    private Map<String, String> buildMetadata(Long orderId, String sagaId) {
        return Map.of(
                "orderId", orderId.toString(),
                "sagaId", sagaId
        );
    }
}

