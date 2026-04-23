package com.innowise.orderservice.exception.order;

import com.innowise.orderservice.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class OrderSoftDeleteException extends BusinessException {
    public OrderSoftDeleteException(Long id) {
        super("Cannot delete order with id[%s]".formatted(id), HttpStatus.CONFLICT);
    }
}
