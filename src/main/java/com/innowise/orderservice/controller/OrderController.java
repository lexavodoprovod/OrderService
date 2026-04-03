package com.innowise.orderservice.controller;

import com.innowise.orderservice.dto.OrderRequestDto;
import com.innowise.orderservice.dto.OrderResponseDto;
import com.innowise.orderservice.entity.Status;
import com.innowise.orderservice.service.OrderService;
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

@RestController
@RequestMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OrderController {

    private static final int PAGINATION_SIZE = 15;
    private static final String SORT_BY = "id";

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDto> addOrder(@RequestBody OrderRequestDto orderRequestDto) {
        OrderResponseDto orderResponseDto = orderService.createOrder(orderRequestDto);
        return ResponseEntity.ok(orderResponseDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long id) {
        OrderResponseDto orderResponseDto = orderService.getOrderById(id);
        return ResponseEntity.ok(orderResponseDto);
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponseDto>> getAllOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) List<Status> statuses,
            @PageableDefault(size = PAGINATION_SIZE, sort = SORT_BY) Pageable pageable) {

        Page<OrderResponseDto> orderPage = orderService.getAllOrders(from, to, statuses, pageable);

        return ResponseEntity.ok(orderPage);

    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponseDto> updateOrder(@PathVariable Long id, @RequestBody OrderRequestDto orderRequestDto) {
        OrderResponseDto orderResponseDto = orderService.updateOrder(id, orderRequestDto);
        return ResponseEntity.ok(orderResponseDto);
    }

    @PatchMapping("/{id}/paid")
    public ResponseEntity<OrderResponseDto> setPaidStatus(@PathVariable Long id) {
        OrderResponseDto orderResponseDto = orderService.setPaidStatus(id);
        return ResponseEntity.ok(orderResponseDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        boolean success = orderService.softdeleteOrder(id);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }


}
