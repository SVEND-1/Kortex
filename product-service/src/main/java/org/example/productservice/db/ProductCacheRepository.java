package org.example.productservice.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productservice.api.dto.response.ProductResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductCacheRepository {
    private final RedisTemplate<String, ProductResponse> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "product:";
    private static final long CACHE_TTL_MINUTES = 15;

    public ProductResponse getProduct(Long productId) {
        String key = CACHE_KEY_PREFIX + productId;
        return redisTemplate.opsForValue().get(key);
    }

    public void save(ProductResponse product) {
        String key = CACHE_KEY_PREFIX + product.id();
        redisTemplate.opsForValue().set(key, product, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public void remove(Long productId) {
        String key = CACHE_KEY_PREFIX + productId;
        redisTemplate.delete(key);
    }
}
