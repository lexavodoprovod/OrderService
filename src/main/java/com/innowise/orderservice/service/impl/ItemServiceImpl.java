package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.exception.ItemExistException;
import com.innowise.orderservice.exception.ItemNotFoundException;
import com.innowise.orderservice.exception.ItemNullParameterException;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.repository.ItemDao;
import com.innowise.orderservice.service.ItemService;
import com.innowise.orderservice.specification.ItemSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemDao itemDao;
    private final ItemMapper itemMapper;

    @Override
    public ItemDto createItem(ItemDto itemDto) {
        if(itemDto == null){
            throw new ItemNullParameterException();
        }

        String itemName = itemDto.getName();

        if(itemDao.existsByName(itemName)){
            throw new ItemExistException(itemName);
        }

        Item item = itemMapper.toItem(itemDto);

        Item savedItem = itemDao.save(item);

        return itemMapper.toItemDto(savedItem);
    }

    @Override
    public ItemDto getItemById(Long id) {
        if(id == null){
            throw new ItemNullParameterException();
        }

        Item item = itemDao.findById(id)
                .orElseThrow(()-> new ItemNotFoundException(id));

        return itemMapper.toItemDto(item);
    }

    @Override
    public Page<ItemDto> getAllItems(String name, Pageable pageable) {
        if(pageable == null){
            throw new ItemNullParameterException();
        }

        Specification<Item> itemSpecification = Specification.allOf(ItemSpecification.byName(name));

        Page<Item> itemsPage = itemDao.findAll(itemSpecification, pageable);

        return itemsPage.map(itemMapper::toItemDto);
    }

    @Override
    @Transactional
    public ItemDto updateItem(ItemDto itemDto) {
        if(itemDto == null){
            throw new ItemNullParameterException();
        }

        Long itemId = itemDto.getId();

        Item item = itemDao.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        itemMapper.updateItem(itemDto, item);

        Item savedItem = itemDao.save(item);

        return itemMapper.toItemDto(savedItem);
    }

    @Override
    public boolean deleteItemById(Long id) {
        if(id == null){
            throw new ItemNullParameterException();
        }

        Item item = itemDao.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));

        itemDao.delete(item);

        return true;
    }
}
