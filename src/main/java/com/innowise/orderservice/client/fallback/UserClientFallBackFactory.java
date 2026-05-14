package com.innowise.orderservice.client.fallback;

import com.innowise.orderservice.client.UserClient;
import com.innowise.orderservice.dto.UserDto;
import com.innowise.orderservice.exception.UserServiceException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;


import static com.innowise.orderservice.client.ThrowFeignException.*;

@Component
public class UserClientFallBackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public UserDto getUserById(Long id) {
                throwFeignEx(cause);
                throw new UserServiceException();
            }
        };
    }
}
