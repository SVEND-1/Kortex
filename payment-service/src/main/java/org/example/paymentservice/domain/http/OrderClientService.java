package org.example.paymentservice.domain.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.api.http.OrderFeignClient;
import org.example.rest.OrderRestResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderClientService {
    private final OrderFeignClient orderFeignClient;

    public void setStatus(Long orderId, String status,Long userId) {
        orderFeignClient.updateOrder(orderId, status, userId);
    }

    public List<OrderRestResponse> getOrder(Long orderId) {
        return orderFeignClient.getOrder(orderId);
    }
}
