package com.innowise.orderservice.exception;

import org.springframework.http.HttpStatus;

public class OrderSoftDeleteException extends BusinessException {
    public OrderSoftDeleteException(Long id) {
        super("Cannot delete order with id[%s]".formatted(id), HttpStatus.CONFLICT);
    }
}
