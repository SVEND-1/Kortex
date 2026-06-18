package org.example.paymentservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.api.OrderFeignClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderClientService {
    private final OrderFeignClient orderFeignClient;

    public void setStatus(Long orderId, String status,Long userId) {
        orderFeignClient.updateOrder(orderId, status, userId);
    }
}
