package org.example.paymentservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.db.PaymentEntity;
import org.example.paymentservice.db.PaymentRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.loolzaaa.youkassa.model.Receipt;

@RequiredArgsConstructor
@Slf4j
@Component
public class ReceiptManager {


    private final PaymentRepository paymentRepository;

    public PaymentEntity saveReceipt(String paymentId, Receipt saved) {
        try {
            PaymentEntity paymentEntity = paymentRepository.findByPaymentId(paymentId)
                    .orElseThrow(() -> new EntityNotFoundException("Платеж не найден"));
            paymentEntity.setReceiptId(saved.getId());
            return paymentRepository.save(paymentEntity);
        }catch (Exception e) {
            log.error("Не удалось сохранить чек id={},ex={}",saved.getId(),e.getMessage());
            throw new RuntimeException("Не удалось сохранить чек");
        }
    }


}
