package org.example.paymentservice.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.domain.YooKassaManagar;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import ru.loolzaaa.youkassa.model.Payment;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@Slf4j
@RequiredArgsConstructor
public class PaymentUrlCacheRepository {
    private final RedisTemplate<String, String> redisTemplate;
    private final YooKassaManagar yooKassaManager;
    private final PaymentRepository paymentRepository;

    private static final String PAYMENT_URL_PREFIX = "payment:url:";
    private static final long TTL_SECONDS = 43200;


    public void savePaymentUrl(Long orderId, String url) {
        if (url == null) {
            return;
        }
        String key = PAYMENT_URL_PREFIX + orderId;
        redisTemplate.opsForValue().set(key, url, TTL_SECONDS, TimeUnit.SECONDS);
        log.debug("Payment URL saved for order {}: {}", orderId, url);
    }

    public Optional<String> getPaymentUrl(Long orderId) {
        String key = PAYMENT_URL_PREFIX + orderId;
        String url = redisTemplate.opsForValue().get(key);
        if (url != null) {
            return Optional.of(url);
        }

        return fetchFromYooKassa(orderId);
    }

    private Optional<String> fetchFromYooKassa(Long orderId) {
        try {
            PaymentEntity paymentEntity = paymentRepository.findByOrderId((orderId));
            if (paymentEntity == null) {
                return Optional.empty();
            }

            String paymentId = paymentEntity.getPaymentId();
            Payment payment = yooKassaManager.findPayment(paymentId);
            if (payment == null) {
                return Optional.empty();
            }

            String url = payment.getConfirmation().getConfirmationUrl();
            if (url != null) {
                savePaymentUrl(orderId, url);
                return Optional.of(url);
            }
        } catch (Exception e) {
            log.error("Failed to fetch payment URL from YooKassa for order {}", orderId, e);
        }
        return Optional.empty();
    }
}
