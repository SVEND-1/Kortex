package org.example.paymentservice.domain;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ApiException;
import org.example.paymentservice.api.dto.response.payment.PaymentCreateResponse;
import org.example.paymentservice.api.dto.response.payment.PaymentPageResponse;
import org.example.paymentservice.db.PaymentEntity;
import org.example.paymentservice.db.PaymentRepository;
import org.example.paymentservice.db.PaymentUrlCacheRepository;
import org.example.paymentservice.domain.mapper.PaymentMapper;
import org.example.rest.OrderRestResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.loolzaaa.youkassa.client.ApiClient;
import ru.loolzaaa.youkassa.client.ApiClientBuilder;
import ru.loolzaaa.youkassa.model.Payment;
import ru.loolzaaa.youkassa.processors.PaymentProcessor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentYooKassaService yooKassaManager;
    private final PaymentManager paymentManager;
    private final OrderClientService orderClientService;

    private final PaymentUrlCacheRepository paymentUrlCacheRepository;

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


    public PaymentPageResponse findAllPaymentsByUser(Long userId, int page, int size) {//Сортировать по дате
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentEntity> paymentEntities = paymentRepository.findAllByUserId(userId, pageable);
        return paymentMapper.toPageResponse(paymentEntities);
    }

    public String getUrlPayment(Long orderId){
        return paymentUrlCacheRepository.getPaymentUrl(orderId).orElse(null);
    }

    @Transactional
    public PaymentCreateResponse createPayment(Long orderId,Long userId,String sagaId) {
        String idempotencyKey = UUID.randomUUID().toString();
        try {
            List<OrderRestResponse> order = orderClientService.getOrder(orderId);
            BigDecimal amount = order.stream().map(el -> el.price().multiply(new BigDecimal(el.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal yookassaAmount = amount.setScale(2, RoundingMode.HALF_UP);
            String value = yookassaAmount.toPlainString();
            Payment saved = yooKassaManager.createYooKassaPayment(paymentProcessor,idempotencyKey,value,orderId,sagaId);

            paymentManager.savePayment(idempotencyKey,saved,yookassaAmount,userId,orderId);

            String url = saved.getConfirmation().getConfirmationUrl();
            paymentUrlCacheRepository.savePaymentUrl(orderId,url);

            log.info("ССЫЛКА {}",url);
            return new PaymentCreateResponse(
                    saved.getId(),
                    url,
                    orderId
            );
        } catch (ApiException e) {
            log.error("Ошибка создания платежа: {}", e.getMessage());
            throw new RuntimeException("Не удалось создать платеж", e);
        }
    }


    public PaymentEntity findByPaymentId(String paymentId){
        return paymentRepository.findByPaymentId(paymentId).orElseThrow(() -> new EntityNotFoundException("Платеж не найден"));
    }

    public PaymentEntity save(PaymentEntity payment) {
        return paymentRepository.save(payment);
    }

}
