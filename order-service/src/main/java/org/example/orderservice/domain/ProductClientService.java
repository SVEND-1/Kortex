package org.example.orderservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.api.ProductFeignClient;
import org.example.orderservice.db.ProductCacheRepository;
import org.example.rest.ProductResponse;
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
