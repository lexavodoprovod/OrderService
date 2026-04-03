package com.innowise.orderservice.client;

import com.innowise.orderservice.client.fallback.UserClientFallBack;
import com.innowise.orderservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-client",
        url = "${USER_SERVICE_URL}",
        path = "/users")
public interface UserClient {

    @GetMapping("/{id}")
    UserDto getUserById(@PathVariable Long id);
}
