package org.example.paymentservice.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "${services.order.url}")
public interface OrderFeignClient {

    @PostMapping("/api/orders/status")
    void updateOrder(@RequestParam Long orderId,
                     @RequestParam String status,
                     @RequestHeader(value = "X-User-Id", required = false) Long userId);
}
