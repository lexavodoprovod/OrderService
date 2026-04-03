package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.ItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing product items within the order service.
 * Provides methods for performing CRUD operations and searching items.
 */
public interface ItemService {

    /**
     * Creates and persists a new item based on the provided DTO.
     *
     * @param itemDto the data transfer object containing the details of the item to be created.
     * @return the created {@link ItemDto} including its generated unique identifier.
     */
    ItemDto createItem(ItemDto itemDto);

    /**
     * Retrieves an item by its unique identifier.
     *
     * @param id the unique identifier of the item to retrieve.
     * @return the {@link ItemDto} representing the found item.
     * @throws com.innowise.orderservice.exception.ItemNotFoundException if no item is found with the given ID.
     */
    ItemDto getItemById(Long id);

    /**
     * Retrieves a paginated list of items, optionally filtered by name.
     *
     * @param name the optional name filter (partial match); if null or empty, all items are returned.
     * @param pageable pagination and sorting information.
     * @return a {@link Page} of {@link ItemDto} objects matching the search criteria.
     */
    Page<ItemDto> getAllItems(String name, Pageable pageable);

    /**
     * Updates an existing item with the provided data.
     *
     * @param itemDto the data transfer object containing updated information and a valid ID.
     * @return the updated {@link ItemDto}.
     * @throws com.innowise.orderservice.exception.ItemNotFoundException if the item to update does not exist.
     */
    ItemDto updateItem(ItemDto itemDto);

    /**
     * Performs a hard or soft deletion of an item identified by its ID.
     *
     * @param id the unique identifier of the item to be deleted.
     * @return true if the item was successfully deleted, false if the item was not found.
     */
    boolean deleteItemById(Long id);
}