package com.innowise.orderservice.exception;

import org.springframework.http.HttpStatus;

public class OrderNullParametrException extends BusinessException {
    public OrderNullParametrException() {
        super("Try to use null parameter in OrderServiceImpl", HttpStatus.BAD_REQUEST);
    }
}
