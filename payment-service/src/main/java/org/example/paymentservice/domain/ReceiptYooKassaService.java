package org.example.paymentservice.domain;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.api.dto.response.receipt.ReceiptResponse;
import org.example.paymentservice.db.PaymentEntity;
import org.example.paymentservice.db.PaymentRepository;
import org.example.paymentservice.domain.mapper.ReceiptMapper;
import org.example.rest.OrderRestResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.loolzaaa.youkassa.client.ApiClient;
import ru.loolzaaa.youkassa.client.ApiClientBuilder;
import ru.loolzaaa.youkassa.model.Payment;
import ru.loolzaaa.youkassa.model.Receipt;
import ru.loolzaaa.youkassa.pojo.Amount;
import ru.loolzaaa.youkassa.pojo.Customer;
import ru.loolzaaa.youkassa.pojo.Item;
import ru.loolzaaa.youkassa.pojo.Settlement;
import ru.loolzaaa.youkassa.processors.PaymentProcessor;
import ru.loolzaaa.youkassa.processors.ReceiptProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReceiptYooKassaService {

    private final PaymentRepository paymentRepository;
    private final YooKassaManagar yooKassaManagar;
    private final ReceiptMapper receiptMapper;
    private final OrderClientService orderClientService;

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
        receiptProcessor = new ReceiptProcessor(apiClient);

        log.info("YooKassa инициализирована");
    }

    @Transactional
    public Receipt createYooKassaReceipt(String paymentId,String email) {
        try {
            Payment payment = yooKassaManagar.findPayment(paymentId);

            if (!"succeeded".equals(payment.getStatus())) {
                throw new IllegalStateException("Чек можно создавать только для успешных платежей");
            }

            PaymentEntity paymentEntity = paymentRepository.findByPaymentId(paymentId)
                    .orElseThrow(() -> new EntityNotFoundException("Платеж не найден"));
            List<OrderRestResponse> order = orderClientService.getOrder(paymentEntity.getOrderId());

            Customer customer = Customer.builder()
                    .email(email)
                    .build();

            List<Item> items = order.stream()
                    .map(orderItem -> Item.builder()
                            .description("Товар #" + orderItem.productId())
                            .amount(Amount.builder()
                                    .value(orderItem.price().toString())
                                    .currency(payment.getAmount().getCurrency())
                                    .build())
                            .quantity(orderItem.quantity().toString())
                            .vatCode(1)
                            .build())
                    .toList();

            BigDecimal totalItemsSum = order.stream()
                    .map(el -> el.price().multiply(new BigDecimal(el.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Amount settlementAmount = Amount.builder()
                    .value(totalItemsSum.toString())
                    .currency(payment.getAmount().getCurrency())
                    .build();

            Settlement settlement = Settlement.builder()
                    .type(getSettlementType(payment))
                    .amount(settlementAmount)
                    .build();

            Receipt receipt = Receipt.builder()
                    .type(Receipt.Type.PAYMENT)
                    .paymentId(paymentId)
                    .customer(customer)
                    .items(items)
                    .settlements(List.of(settlement))
                    .send(true)
                    .build();

            return receiptProcessor.create(receipt, null);
        } catch (Exception e) {
            log.error("Не удалось создать чек через yookassa,ex={}", e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public ReceiptResponse findReceiptDTO(String paymentId) {//Надо чтобы не dto
        try {
            PaymentEntity payment = paymentRepository.findByPaymentId(paymentId)
                    .orElseThrow(() -> new EntityNotFoundException("Платеж не найден"));

            if (payment.getReceiptId() == null) {
                log.warn("Чек ещё не создан");
                return new ReceiptResponse(
                        null,null,null,null,null,null,null,null,null,null,null,null,null
                );//TODO переделать весь метод у payment есть сохранение чека
            }

            Receipt receipt = receiptProcessor.findById(payment.getReceiptId());
            return receiptMapper.convertReceiptToReceiptResponse(receipt,String.valueOf(payment.getAmount()));
        } catch (Exception e) {
            log.error("Ошибка поиска чека,ex={}", e.getMessage());
            throw new RuntimeException("Чек не найден", e);
        }
    }

    private String getSettlementType(Payment payment) {
        return switch (payment.getPaymentMethod().getType()) {
            case "bank_card", "sberbank", "tinkoff_bank", "alpha_bank", "sbp" ->
                    Settlement.Type.CASHLESS;
            case "yoo_money", "qiwi" ->
                    Settlement.Type.PREPAYMENT;
            case "cash" ->
                    Settlement.Type.PAYOUT;
            case "bank_transfer" ->
                    Settlement.Type.POSTPAYMENT;
            default ->
                    Settlement.Type.CASHLESS;
        };
    }
}
