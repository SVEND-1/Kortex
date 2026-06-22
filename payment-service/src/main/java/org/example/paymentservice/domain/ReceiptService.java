package org.example.paymentservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ApiException;
import org.example.paymentservice.api.dto.response.receipt.ReceiptResponse;
import org.example.paymentservice.api.exception.PaymentOwnershipException;
import org.example.paymentservice.db.PaymentEntity;
import org.example.paymentservice.db.PaymentRepository;
import org.example.paymentservice.domain.mapper.ReceiptMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.loolzaaa.youkassa.model.Receipt;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {
    public final ReceiptYooKassaService receiptYooKassaService;
    private final PaymentRepository paymentRepository;
    private final ReceiptMapper receiptMapper;
    private final ReceiptManager receiptManager;

    @Transactional
    public ReceiptResponse createReceipt(String paymentId,String email,Long userId) {
        isValidUser(paymentId,userId);
        try {
            Receipt saved = receiptYooKassaService.createYooKassaReceipt(paymentId,email);
            PaymentEntity payment = receiptManager.saveReceipt(paymentId,saved);
            return receiptMapper.convertReceiptToReceiptResponse(saved,String.valueOf(payment.getAmount()));
        } catch (ApiException e) {
            log.error("Ошибка создания чека для платежа {}: {}", paymentId, e.getMessage());
            throw new RuntimeException("Не удалось создать чек", e);
        }
    }

    public ReceiptResponse findReceipt(String paymentId,Long userId) {
        isValidUser(paymentId,userId);
        return receiptYooKassaService.findReceiptDTO(paymentId);
    }

    public void isValidUser(String paymentId,Long userId) {
        PaymentEntity payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Платеж не найден"));
        if(!payment.getUserId().equals(userId)){
            log.warn("Пользователь не является владельцем платежа");
            throw new PaymentOwnershipException("Пользователь не является владельцем платежа");
        }
    }
}
