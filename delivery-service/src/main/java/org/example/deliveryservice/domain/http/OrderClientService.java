package org.example.deliveryservice.domain.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryservice.api.feignClient.OrderFeignClient;
import org.example.deliveryservice.db.OrderStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderClientService {

    public final OrderFeignClient orderFeignClient;

    public void setStatus(Long orderId, OrderStatus status,Long userId) {
        orderFeignClient.updateOrder(orderId, String.valueOf(status),userId);
    }
}
