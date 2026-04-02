package com.innowise.orderservice.exception;

import org.springframework.http.HttpStatus;

public class ItemNullParameterException extends BusinessException {
    public ItemNullParameterException() {
        super("Try to use null parameter in ItemServiceImpl", HttpStatus.BAD_REQUEST);
    }
}
