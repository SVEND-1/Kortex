package org.example.paymentservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.api.dto.response.payment.PaymentResponse;
import org.example.paymentservice.db.PaymentEntity;
import org.example.paymentservice.db.PaymentRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.loolzaaa.youkassa.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
@Component
public class PaymentManager {

    private final PaymentRepository paymentRepository;
//    private final PaymentService paymentService;

    public void savePayment(String idempotencyKey, Payment saved, BigDecimal amount, Long userId,Long orderId) {
        try {
            PaymentEntity paymentEntity = PaymentEntity.builder()
                    .idempotencyKey(idempotencyKey)
                    .userId(userId)
                    .paymentId(saved.getId())
                    .paid(false)
                    .amount(amount)
                    .orderId(orderId)
                    .createdAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(paymentEntity);
        }catch (Exception e) {
            log.error("Не удалось сохранить платеж paymentId={},ex={}", saved.getId(), e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }


//    public Page<PaymentResponse> findAllPaymentsByUser(Long userId,int page, int size) {
//        try {
//            Pageable pageable = PageRequest.of(page, size);
//
//            Page<PaymentEntity> userPayments = paymentRepository
//                    .findAllByUserId(userId, pageable);
//
//            return userPayments.map(el -> paymentService.findPaymentDto(el.getPaymentId(),userId));
//        }catch (Exception e) {
//            log.error("Не удалось загрузить страницу с платежами, ex={}", e.getMessage());
//            throw new RuntimeException(e.getMessage());
//        }
//    }
}
