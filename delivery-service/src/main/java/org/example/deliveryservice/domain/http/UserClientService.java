package org.example.deliveryservice.domain.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryservice.api.feignClient.UserFeignClient;
import org.example.rest.AddressRestResponse;
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
