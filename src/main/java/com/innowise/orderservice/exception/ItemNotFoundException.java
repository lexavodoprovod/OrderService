package com.innowise.orderservice.exception;

public class ItemNotFoundException extends EntityNotFoundException {
    public ItemNotFoundException(Long id) {
        super("Could not find item with id[%s] ".formatted(id));
    }
}
