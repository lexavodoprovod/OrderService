package com.innowise.orderservice.exception.order;

import com.innowise.orderservice.exception.EntityNotFoundException;

public class OrderNotFoundException extends EntityNotFoundException {
    public OrderNotFoundException(Long id) {
        super("Could not find order with id[%s] ".formatted(id));
    }
}
