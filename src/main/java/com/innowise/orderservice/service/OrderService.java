package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.OrderRequestDto;
import com.innowise.orderservice.dto.OrderResponseDto;
import com.innowise.orderservice.entity.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    OrderResponseDto createOrder(OrderRequestDto requestOrderDto);
    OrderResponseDto getOrderById(Long id);
    Page<OrderResponseDto> getAllOrders(LocalDate from, LocalDate to, List<Status> statuses, Pageable pageable);
    OrderResponseDto setPaidStatus(Long id );
    OrderResponseDto updateOrder(Long id, OrderRequestDto requestOrderDto);
    boolean softdeleteOrder(Long id);
}
