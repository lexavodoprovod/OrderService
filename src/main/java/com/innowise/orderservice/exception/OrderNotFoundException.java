package com.innowise.orderservice.exception;

public class OrderNotFoundException extends EntityNotFoundException {
    public OrderNotFoundException(Long id) {
        super("Could not find order with id[%s] ".formatted(id));
    }
}
