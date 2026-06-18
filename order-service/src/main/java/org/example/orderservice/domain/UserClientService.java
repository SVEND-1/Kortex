package org.example.orderservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.api.UserFeignClient;
import org.example.orderservice.api.dto.AddressRestResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserClientService {

    private final UserFeignClient userFeignClient;

    public AddressRestResponse getAddress(Long id, Long userId) {
        return userFeignClient.getUserById(id,userId);
    }
}
