package com.innowise.orderservice.exception.item;

import com.innowise.orderservice.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ItemExistException extends BusinessException {
    public ItemExistException(String name) {
        super("Item with this name [%s] already exist".formatted(name), HttpStatus.CONFLICT);
    }
}
