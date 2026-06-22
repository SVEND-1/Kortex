package org.example.orderservice.api.http;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "delivery-service", url = "${services.delivery.url}")
public interface DeliveryFeignClient {

    @PostMapping("/api/delivery/status-pending")
    void statusPending(@RequestParam Long orderId);
}
