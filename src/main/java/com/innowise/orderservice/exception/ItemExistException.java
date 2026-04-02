package com.innowise.orderservice.exception;

import org.springframework.http.HttpStatus;

public class ItemExistException extends BusinessException {
    public ItemExistException(String name) {
        super("Item with this name [%s] already exist".formatted(name), HttpStatus.CONFLICT);
    }
}
