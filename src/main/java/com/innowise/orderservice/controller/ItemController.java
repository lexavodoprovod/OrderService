package com.innowise.orderservice.controller;

import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing product items.
 * Provides endpoints for basic CRUD operations with support for pagination and filtering.
 * * All responses are returned in {@value MediaType#APPLICATION_JSON_VALUE} format.
 */
@RestController
@RequestMapping(value = "/items", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ItemController {

    private static final int PAGINATION_SIZE = 15;
    private static final String SORT_BY = "id";

    private final ItemService itemService;

    /**
     * Creates a new item.
     *
     * @param itemDto the item data to be saved, validated by {@link Valid}.
     * @return {@link ResponseEntity} containing the created {@link ItemDto} and HTTP 201 CREATED.
     */
    @PostMapping
    public ResponseEntity<ItemDto> createItem(@Valid @RequestBody ItemDto itemDto) {
        ItemDto item = itemService.createItem(itemDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    /**
     * Retrieves a single item by its ID.
     *
     * @param id the unique identifier of the item.
     * @return {@link ResponseEntity} with the found {@link ItemDto} and HTTP 200 OK.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getItemById(@PathVariable Long id) {
        ItemDto itemDto = itemService.getItemById(id);
        return ResponseEntity.ok(itemDto);
    }

    /**
     * Retrieves a paginated list of items with optional name filtering.
     *
     * @param name     optional search string for filtering items by name (partial match).
     * @param pageable pagination parameters (page, size, sort), defaults to size {@value #PAGINATION_SIZE} and sort by {@value #SORT_BY}.
     * @return {@link ResponseEntity} containing a {@link Page} of {@link ItemDto}.
     */
    @GetMapping
    public ResponseEntity<Page<ItemDto>> getAllItems(
            @RequestParam(required = false) String name,
            @PageableDefault(size = PAGINATION_SIZE, sort = SORT_BY) Pageable pageable
    ) {
        Page<ItemDto> page = itemService.getAllItems(name, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Updates an existing item.
     *
     * @param itemDto the item data with an existing ID to update.
     * @return {@link ResponseEntity} with the updated {@link ItemDto}.
     */
    @PutMapping
    public ResponseEntity<ItemDto> updateItem(@Valid @RequestBody ItemDto itemDto) {
        ItemDto item = itemService.updateItem(itemDto);
        return ResponseEntity.ok(item);
    }

    /**
     * Deletes an item by its unique identifier.
     *
     * @param id the unique identifier of the item to delete.
     * @return {@link ResponseEntity} with HTTP 204 No Content if successful, or HTTP 404 Not Found if item does not exist.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItemById(@PathVariable Long id) {
        boolean success = itemService.deleteItemById(id);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}