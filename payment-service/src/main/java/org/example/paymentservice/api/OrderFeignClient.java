package org.example.paymentservice.api;

import org.example.rest.OrderRestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "order-service", url = "${services.order.url}")
public interface OrderFeignClient {

    @PostMapping("/api/orders/status")
    void updateOrder(@RequestParam Long orderId,
                     @RequestParam String status,
                     @RequestHeader(value = "X-User-Id", required = false) Long userId);

    @GetMapping("/api/orders/{orderId}")
    List<OrderRestResponse> getOrder(
            @PathVariable Long orderId
    );
}
