package org.example.paymentservice.domain;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.loolzaaa.youkassa.client.ApiClient;
import ru.loolzaaa.youkassa.client.ApiClientBuilder;
import ru.loolzaaa.youkassa.model.Payment;
import ru.loolzaaa.youkassa.processors.PaymentProcessor;
import ru.loolzaaa.youkassa.processors.ReceiptProcessor;

@Slf4j
@RequiredArgsConstructor
@Component
public class YooKassaManagar {

    private final PaymentYooKassaService yooKassaManager;
    private PaymentProcessor paymentProcessor;
    private ReceiptProcessor receiptProcessor;
    private ApiClient apiClient;
    @Value("${shop_id}")
    private String shopId;

    @Value("${payment_key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        apiClient = ApiClientBuilder.newBuilder()
                .configureBasicAuth(shopId, secretKey)
                .build();
        paymentProcessor = new PaymentProcessor(apiClient);
        receiptProcessor = new ReceiptProcessor(apiClient);

        log.info("YooKassa инициализирована");
    }

    public Payment findPayment(String paymentId) {
        return yooKassaManager.findPayment(paymentProcessor,paymentId);
    }
}
