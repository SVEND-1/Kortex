package org.example.cartservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cartservice.api.ProductFeignClient;
import org.example.cartservice.api.dto.response.ProductResponse;
import org.example.cartservice.db.ProductCacheRepository;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductClientService {

    private final ProductCacheRepository productCacheRepository;
    private final ProductFeignClient productFeignClient;

    public ProductResponse getProduct(Long productId) {
        ProductResponse cached = productCacheRepository.get(productId);
        if (cached != null) {
            log.debug("Cache hit для productId={}", productId);
            return cached;
        }

        try {
            ProductResponse product = productFeignClient.getById(productId);
            productCacheRepository.save(product);
            return product;
        } catch (Exception e) {
            log.error("Не удалось получить продукт productId={}: {}", productId, e.getMessage());
            throw new EntityNotFoundException("Продукт не найден: " + productId);
        }
    }
}
