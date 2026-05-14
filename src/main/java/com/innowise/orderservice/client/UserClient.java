package com.innowise.orderservice.client;

import com.innowise.orderservice.client.fallback.UserClientFallBackFactory;
import com.innowise.orderservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        path = "/users",
        fallbackFactory = UserClientFallBackFactory.class)
public interface UserClient {

    @GetMapping("/{id}")
    UserDto getUserById(@PathVariable Long id);
}
