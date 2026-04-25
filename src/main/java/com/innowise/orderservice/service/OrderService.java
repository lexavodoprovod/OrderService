package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.request.OrderRequestDto;
import com.innowise.orderservice.dto.resoponse.OrderResponseDto;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.exception.order.OrderNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for managing customer orders.
 * Handles order lifecycle including creation, payment processing,
 * sophisticated filtering, and soft deletion.
 */
public interface OrderService {

    /**
     * Creates a new order and calculates the total price based on item prices and quantities.
     * Integrates with External User Service to validate the order owner.
     *
     * @param requestOrderDto the data transfer object containing user ID and a list of items with quantities.
     * @return the {@link OrderResponseDto} representing the newly created order with calculated totals.
     */
    OrderResponseDto createOrder(OrderRequestDto requestOrderDto);

    /**
     * Retrieves detailed information about a specific order by its identifier.
     *
     * @param id the unique identifier of the order to retrieve.
     * @return the {@link OrderResponseDto} containing order details and item information.
     * @throws OrderNotFoundException if the order does not exist.
     */
    OrderResponseDto getOrderById(Long id);

    /**
     * Retrieves a paginated list of orders by user identifier.
     * @param userId user identifier
     * @param pageable pagination and sorting configuration.
     * @return a {@link Page} of {@link OrderResponseDto}.
     */
    Page<OrderResponseDto> getOrdersByUserId(Long userId, Pageable pageable);

    /**
     * Retrieves a paginated list of orders filtered by various criteria.
     *
     * @param from     the start date of the period to filter orders (inclusive).
     * @param to       the end date of the period to filter orders (inclusive).
     * @param statuses a list of {@link OrderStatus} to filter by; returns all if null or empty.
     * @param pageable pagination and sorting configuration.
     * @return a {@link Page} of {@link OrderResponseDto} matching the provided filters.
     */
    Page<OrderResponseDto> getAllOrders(LocalDate from, LocalDate to, List<OrderStatus> statuses, Pageable pageable);

    /**
     * Transitions the order status to PAID.
     * This method typically includes business logic validation to ensure
     * the transition from the current status is valid.
     *
     * @param id the unique identifier of the order to be paid.
     * @return the {@link OrderResponseDto} with the updated status.
     * @throws IllegalStateException if the order is in a state that cannot be transitioned to PAID.
     */
    OrderResponseDto setPaidStatus(Long id);

    /**
     * Transitions the order status to CANCELLED.
     *
     * @param id the unique identifier of the order to be cancelled.
     * @return the {@link OrderResponseDto} with the updated status.
     * @throws IllegalStateException if the order is in a state that cannot be transitioned to CANCELLED.
     */
    OrderResponseDto setCancelledStatus(Long id);

    /**
     * Updates an existing order's composition and re-calculates the total price.
     * Old order items are replaced with the new ones provided in the request.
     *
     * @param id               the identifier of the order to update.
     * @param requestOrderDto the new order data including the updated list of items.
     * @return the {@link OrderResponseDto} reflecting the changes.
     */
    OrderResponseDto updateOrder(Long id, OrderRequestDto requestOrderDto);

    /**
     * Marks an order as deleted without removing it from the database (Soft Delete).
     * The order will be excluded from general search results but preserved for historical records.
     *
     * @param id the unique identifier of the order to mark as deleted.
     * @return true if the order was successfully marked as deleted, false if the order was not found.
     */
    boolean softDeleteOrder(Long id);
}