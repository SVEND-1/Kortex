package org.example.cartservice.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cartservice.api.dto.response.ProductResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductCacheRepository {
    private final RedisTemplate<String, ProductResponse> redisTemplate;

    private static final String CACHE_KEY_PREFIX  = "product:";
    private static final long   CACHE_TTL_MINUTES = 15;

    public ProductResponse get(Long productId) {
        try {
            Object value = redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + productId);
            if (value instanceof ProductResponse product) {
                log.debug("Продукт {} получен из Redis", productId);
                return product;
            }
            return null;
        } catch (Exception e) {
            log.warn("Ошибка чтения из Redis для productId={}: {}", productId, e.getMessage());
            return null;
        }
    }

    public void save(ProductResponse product) {
        try {
            redisTemplate.opsForValue()
                    .set(CACHE_KEY_PREFIX + product.id(), product,
                            CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Ошибка сохранения в Redis для productId={}: {}", product.id(), e.getMessage());
        }
    }
}
