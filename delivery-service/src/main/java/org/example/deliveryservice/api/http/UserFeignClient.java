package org.example.deliveryservice.api.http;

import org.example.rest.AddressRestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", url = "${services.user.url}")
public interface UserFeignClient {

    @GetMapping("/api/users/{id}/address")
    AddressRestResponse getUserById(@PathVariable("id") Long id, @RequestHeader(value = "X-User-Id", required = false) Long userId);

}
