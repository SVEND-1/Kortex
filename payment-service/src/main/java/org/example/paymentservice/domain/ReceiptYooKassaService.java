package org.example.paymentservice.domain;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.db.PaymentEntity;
import org.example.paymentservice.db.PaymentRepository;
import org.example.paymentservice.domain.http.OrderClientService;
import org.example.rest.OrderRestResponse;
import org.springframework.beans.factory.annotation.Value;
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
import ru.loolzaaa.youkassa.processors.ReceiptProcessor;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReceiptYooKassaService {

    private final PaymentRepository paymentRepository;
    private final YooKassaManagar yooKassaManagar;
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

            Customer customer = buildCustomer(email);
            List<Item> items = buildItems(order, payment);
            BigDecimal totalItemsSum = calculateTotalAmount(order);
            Amount settlementAmount = buildAmount(totalItemsSum, payment);
            Settlement settlement = buildSettlement(settlementAmount,payment);

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

    private Customer buildCustomer(String email) {
        return Customer.builder()
                .email(email)
                .build();
    }

    private List<Item> buildItems(List<OrderRestResponse> order,Payment payment) {
        return order.stream()
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
    }

    private BigDecimal calculateTotalAmount(List<OrderRestResponse> order) {
        return order.stream()
                .map(el -> el.price().multiply(new BigDecimal(el.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Amount buildAmount(BigDecimal totalAmount, Payment payment) {
        return Amount.builder()
                .value(totalAmount.toString())
                .currency(payment.getAmount().getCurrency())
                .build();
    }

    private Settlement buildSettlement(Amount amount,Payment payment) {
        return Settlement.builder()
                .type(getSettlementType(payment))
                .amount(amount)
                .build();
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
