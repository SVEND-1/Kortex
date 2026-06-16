package org.example.orderservice.api;

import org.example.orderservice.api.dto.UserRestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", url = "${services.user.url}")
public interface UserFeignClient {

    @GetMapping("/api/users/{id}/address")
    UserRestResponse getUserById(@PathVariable("id") Long id, @RequestHeader("X-User-Role") String role);
}
