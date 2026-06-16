package org.example.paymentservice.domain;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ApiException;
import org.example.paymentservice.api.dto.response.payment.PaymentCreateResponse;
import org.example.paymentservice.api.dto.response.payment.PaymentPageResponse;
import org.example.paymentservice.api.dto.response.payment.PaymentResponse;
import org.example.paymentservice.api.exception.PaymentOwnershipException;
import org.example.paymentservice.db.PaymentEntity;
import org.example.paymentservice.db.PaymentRepository;
import org.example.paymentservice.domain.mapper.PaymentMapper;
import org.example.paymentservice.domain.mapper.ReceiptMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.loolzaaa.youkassa.client.ApiClient;
import ru.loolzaaa.youkassa.client.ApiClientBuilder;
import ru.loolzaaa.youkassa.model.Payment;
import ru.loolzaaa.youkassa.processors.PaymentProcessor;
import ru.loolzaaa.youkassa.processors.ReceiptProcessor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final YooKassaManager yooKassaManager;
    private final PaymentManager paymentManager;
    private final ReceiptManager receiptManager;
    private final ReceiptMapper receiptMapper;

    @Value("${shop_id}")
    private String shopId;

    @Value("${payment_key}")
    private String secretKey;

    private ApiClient apiClient;

    private PaymentProcessor paymentProcessor;
    private ReceiptProcessor receiptProcessor;


    @PostConstruct
    public void init() {
        apiClient = ApiClientBuilder.newBuilder()
                .configureBasicAuth(shopId, secretKey)
                .build();
        paymentProcessor = new PaymentProcessor(apiClient);
        receiptProcessor = new ReceiptProcessor(apiClient);

        log.info("YooKassa инициализирована");
    }

    public PaymentResponse findPaymentDto(String paymentId,Long userId) {
        isValidUser(paymentId,userId);
        return paymentMapper.convertEntityToPaymentResponse(findPayment(paymentId));
    }

    public PaymentPageResponse findAllPaymentsByUser(Long userId, int page, int size) {
        return paymentMapper.toPageResponse(paymentManager.findAllPaymentsByUser(userId,page,size));
    }

//    public ReceiptResponse findReceipt(String paymentId){
//        isValidUser(paymentId);
//        return yooKassaManager.findReceiptDTO(receiptProcessor,paymentId);
//    }

    @Transactional
    public PaymentCreateResponse createPayment(BigDecimal amount,Long orderId,Long userId) {
        String idempotencyKey = UUID.randomUUID().toString();
        try {
            BigDecimal yookassaAmount = amount.setScale(2, RoundingMode.HALF_UP);
            String value = yookassaAmount.toPlainString();
            Payment saved = yooKassaManager.createYooKassaPayment(paymentProcessor,idempotencyKey,value,orderId);

            paymentManager.savePayment(idempotencyKey,saved,yookassaAmount,userId);

            return new PaymentCreateResponse(
                    saved.getId(),
                    saved.getConfirmation().getConfirmationUrl(),
                    orderId
            );
        } catch (ApiException e) {
            log.error("Ошибка создания платежа: {}", e.getMessage());
            throw new RuntimeException("Не удалось создать платеж", e);
        }
    }

//    @Transactional
//    public ReceiptResponse createReceipt(String paymentId) {
//        isValidUser(paymentId);
//        try {
//            Receipt saved = yooKassaManager.createYooKassaReceipt(receiptProcessor,paymentId);
//            receiptManager.saveReceipt(paymentId,saved);
//            return receiptMapper.convertReceiptToReceiptResponse(saved);
//        } catch (ApiException e) {
//            log.error("Ошибка создания чека для платежа {}: {}", paymentId, e.getMessage());
//            throw new RuntimeException("Не удалось создать чек", e);
//        }
//    }


    public PaymentEntity findByPaymentId(String paymentId){
        return paymentRepository.findByPaymentId(paymentId);
    }

    public Payment findPayment(String paymentId) {
        return yooKassaManager.findPayment(paymentProcessor,paymentId);
    }

    public PaymentEntity save(PaymentEntity payment) {
        return paymentRepository.save(payment);
    }


    public void isValidUser(String paymentId,Long userId) {
        if(!findByPaymentId(paymentId).getUserId().equals(userId)){
            log.warn("Пользователь не является владельцем платежа");
            throw new PaymentOwnershipException("Пользователь не является владельцем платежа");
        }
    }
}
