package com.innowise.orderservice.controller;

import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping(value = "/items", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ItemController {

    private static final int PAGINATION_SIZE = 15;
    private static final String SORT_BY = "id";

    private final ItemService itemService;


    @PostMapping
    public ResponseEntity<ItemDto> createItem(@RequestBody ItemDto itemDto) {
        ItemDto item =  itemService.createItem(itemDto);
        return ResponseEntity.ok(item);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getItemById(@PathVariable Long id) {
        ItemDto itemDto = itemService.getItemById(id);
        return ResponseEntity.ok(itemDto);
    }

    @GetMapping
    public ResponseEntity<Page<ItemDto>> getAllItems(
            @RequestParam(required = false) String name,
            @PageableDefault(size = PAGINATION_SIZE, sort = SORT_BY) Pageable pageable
    ) {
        Page<ItemDto> page = itemService.getAllItems(name, pageable);
        return ResponseEntity.ok(page);
    }

    @PutMapping
    public ResponseEntity<ItemDto> updateItem(@RequestBody ItemDto itemDto) {
        ItemDto item =  itemService.updateItem(itemDto);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItemById(@PathVariable Long id) {
        boolean success = itemService.deleteItemById(id);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
