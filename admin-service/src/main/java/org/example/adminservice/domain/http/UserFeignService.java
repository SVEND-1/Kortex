package org.example.adminservice.domain.http;

import lombok.RequiredArgsConstructor;
import org.example.adminservice.api.http.UserFeignClient;
import org.example.rest.UserRestResponse;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserFeignService {

    private final UserFeignClient userFeignClient;

    public UserRestResponse findUserDTO(Long userId){
        return userFeignClient.getUserById(userId,"ADMIN");
    }
}
