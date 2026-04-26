package com.innowise.orderservice.exception.order;

import com.innowise.orderservice.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class OrderNullParameterException extends BusinessException {
    public OrderNullParameterException() {
        super("Try to use null parameter in OrderServiceImpl", HttpStatus.BAD_REQUEST);
    }
}
