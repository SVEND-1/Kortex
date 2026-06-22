package org.example.deliveryservice.domain.http;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryservice.api.feignClient.ProductFeignClient;
import org.example.rest.ProductNoImageRestResponse;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductClientService {

    private final ProductFeignClient productFeignClient;

    public ProductNoImageRestResponse getProduct(Long productId) {
        try {
            return productFeignClient.getById(productId);
        } catch (Exception e) {
            log.error("Не удалось получить продукт productId={}: {}", productId, e.getMessage());
            throw new EntityNotFoundException("Продукт не найден: " + productId);
        }
    }
}
