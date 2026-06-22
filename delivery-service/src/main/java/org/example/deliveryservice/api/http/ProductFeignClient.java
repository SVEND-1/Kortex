package org.example.deliveryservice.api.http;

import org.example.rest.ProductNoImageRestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${services.product.url}")
public interface ProductFeignClient {

    @GetMapping("/api/products/{id}/no-image")
    ProductNoImageRestResponse getById(@PathVariable("id") Long id);
}
