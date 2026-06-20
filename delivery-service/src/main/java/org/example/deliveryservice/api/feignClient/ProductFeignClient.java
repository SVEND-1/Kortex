package org.example.deliveryservice.api.feignClient;

import org.example.rest.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${services.product.url}")
public interface ProductFeignClient {

    @GetMapping("/api/products/{id}")
    ProductResponse getById(@PathVariable("id") Long id);
}
