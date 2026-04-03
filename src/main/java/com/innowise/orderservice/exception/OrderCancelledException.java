package com.innowise.orderservice.exception;

import org.springframework.http.HttpStatus;

public class OrderCancelledException extends BusinessException {
    public OrderCancelledException() {
        super("This order is cancelled", HttpStatus.BAD_REQUEST);
    }
}
