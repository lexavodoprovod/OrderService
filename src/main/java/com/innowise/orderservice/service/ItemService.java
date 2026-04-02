package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.ItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemService {
    ItemDto createItem(ItemDto itemDto);
    ItemDto getItemById(Long id);
    Page<ItemDto> getAllItems(String name, Pageable pageable);
    ItemDto updateItem(ItemDto itemDto);
    boolean deleteItemById(Long id);
}
