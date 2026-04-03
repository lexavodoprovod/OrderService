package com.innowise.orderservice.client.fallback;

import com.innowise.orderservice.client.UserClient;
import com.innowise.orderservice.dto.UserDto;
import com.innowise.orderservice.exception.UserServiceException;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallBack implements UserClient {
    @Override
    public UserDto getUserById(Long id) {
        throw new UserServiceException();
    }
}
