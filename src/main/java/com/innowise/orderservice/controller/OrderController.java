package com.innowise.orderservice.controller;

import com.innowise.orderservice.dto.OrderRequestDto;
import com.innowise.orderservice.dto.OrderResponseDto;
import com.innowise.orderservice.entity.Status;
import com.innowise.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for managing customer orders.
 * Provides endpoints for order lifecycle management, including creation, retrieval with
 * complex filtering, state transitions, and soft deletion.
 */
@RestController
@RequestMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OrderController {

    private static final int PAGINATION_SIZE = 15;
    private static final String SORT_BY = "id";

    private final OrderService orderService;

    /**
     * Places a new order in the system.
     *
     * @param orderRequestDto the order details containing user information and items.
     * @return {@link ResponseEntity} containing the created {@link OrderResponseDto} and HTTP 200 OK.
     */
    @PostMapping
    public ResponseEntity<OrderResponseDto> addOrder(@Valid @RequestBody OrderRequestDto orderRequestDto) {
        OrderResponseDto orderResponseDto = orderService.createOrder(orderRequestDto);
        return ResponseEntity.ok(orderResponseDto);
    }

    /**
     * Retrieves an order by its unique identifier.
     *
     * @param id the unique identifier of the order.
     * @return {@link ResponseEntity} with the found {@link OrderResponseDto} and HTTP 200 OK.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long id) {
        OrderResponseDto orderResponseDto = orderService.getOrderById(id);
        return ResponseEntity.ok(orderResponseDto);
    }

    /**
     * Retrieves a paginated and filtered list of orders.
     *
     * @param from     optional start date (inclusive) in ISO format (yyyy-MM-dd).
     * @param to       optional end date (inclusive) in ISO format (yyyy-MM-dd).
     * @param statuses optional list of {@link Status} values to filter by.
     * @param pageable pagination parameters, defaults to size {@value #PAGINATION_SIZE} and sort by {@value #SORT_BY}.
     * @return {@link ResponseEntity} containing a {@link Page} of {@link OrderResponseDto}.
     */
    @GetMapping
    public ResponseEntity<Page<OrderResponseDto>> getAllOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) List<Status> statuses,
            @PageableDefault(size = PAGINATION_SIZE, sort = SORT_BY) Pageable pageable) {

        Page<OrderResponseDto> orderPage = orderService.getAllOrders(from, to, statuses, pageable);
        return ResponseEntity.ok(orderPage);
    }

    /**
     * Updates an existing order's details.
     *
     * @param id              the unique identifier of the order to update.
     * @param orderRequestDto the updated order data.
     * @return {@link ResponseEntity} with the updated {@link OrderResponseDto}.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderResponseDto> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderRequestDto orderRequestDto) {
        OrderResponseDto orderResponseDto = orderService.updateOrder(id, orderRequestDto);
        return ResponseEntity.ok(orderResponseDto);
    }

    /**
     * Updates the status of an existing order to PAID.
     *
     * @param id the unique identifier of the order to be marked as paid.
     * @return {@link ResponseEntity} with the updated {@link OrderResponseDto}.
     */
    @PatchMapping("/{id}/paid")
    public ResponseEntity<OrderResponseDto> setPaidStatus(@PathVariable Long id) {
        OrderResponseDto orderResponseDto = orderService.setPaidStatus(id);
        return ResponseEntity.ok(orderResponseDto);
    }

    /**
     * Performs a soft delete on an order by its ID.
     *
     * @param id the unique identifier of the order to mark as deleted.
     * @return {@link ResponseEntity} with HTTP 204 No Content if successful, or HTTP 404 Not Found if the order does not exist.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        boolean success = orderService.softdeleteOrder(id);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}