package org.example.paymentservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ApiException;
import org.example.paymentservice.api.dto.response.payment.PaymentCreateResponse;
import org.example.paymentservice.api.dto.response.payment.PaymentPageResponse;
import org.example.paymentservice.db.PaymentEntity;
import org.example.paymentservice.db.PaymentRepository;
import org.example.paymentservice.db.PaymentUrlCacheRepository;
import org.example.paymentservice.domain.http.OrderClientService;
import org.example.paymentservice.domain.mapper.PaymentMapper;
import org.example.rest.OrderRestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.loolzaaa.youkassa.model.Payment;

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


    public PaymentPageResponse findAllPaymentsByUser(Long userId, int page, int size) {//Сортировать по дате
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentEntity> paymentEntities = paymentRepository.findAllByUserId(userId, pageable);
        return paymentMapper.toPageResponse(paymentEntities);
    }

    public PaymentEntity findByPaymentId(String paymentId){
        return paymentRepository.findByPaymentId(paymentId).orElseThrow(() -> new EntityNotFoundException("Платеж не найден"));
    }

    public String getUrlPayment(Long orderId){
        return paymentUrlCacheRepository.getPaymentUrl(orderId).orElse(null);
    }

    public PaymentEntity save(PaymentEntity payment) {
        return paymentRepository.save(payment);
    }

    @Transactional
    public PaymentCreateResponse createPayment(Long orderId,Long userId,String sagaId) {
        String idempotencyKey = UUID.randomUUID().toString();
        try {
            List<OrderRestResponse> order = orderClientService.getOrder(orderId);
            BigDecimal amount = calculatePaymentAmount(order);

            BigDecimal yookassaAmount = amount.setScale(2, RoundingMode.HALF_UP);
            String value = yookassaAmount.toPlainString();
            Payment saved = yooKassaManager.createYooKassaPayment(idempotencyKey,value,orderId,sagaId);
            paymentManager.savePayment(idempotencyKey,saved,yookassaAmount,userId,orderId);

            String url = saved.getConfirmation().getConfirmationUrl();
            paymentUrlCacheRepository.savePaymentUrl(orderId,url);

            return new PaymentCreateResponse(saved.getId(), url, orderId);
        } catch (ApiException e) {
            log.error("Ошибка создания платежа: {}", e.getMessage());
            throw new RuntimeException("Не удалось создать платеж", e);
        }
    }

    public BigDecimal calculatePaymentAmount(List<OrderRestResponse> order){
        return order.stream()
                .map(el -> el.price().multiply(new BigDecimal(el.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
