package org.example.orderservice.domain.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.api.http.DeliveryFeignClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryFeignService {


    private final DeliveryFeignClient deliveryFeignClient;

    public void statusPending(Long id){
        deliveryFeignClient.statusPending(id);
    }
}
