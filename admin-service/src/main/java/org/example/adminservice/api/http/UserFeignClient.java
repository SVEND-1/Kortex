package org.example.adminservice.api.http;

import org.example.rest.UserRestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", url = "${services.user.url}")
public interface UserFeignClient {

    @GetMapping("/api/users/{id}")
    UserRestResponse getUserById(@PathVariable("id") Long id, @RequestHeader("X-User-Role") String role);
}
