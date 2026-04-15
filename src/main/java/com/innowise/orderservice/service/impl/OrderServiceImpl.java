package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.client.UserClient;
import com.innowise.orderservice.dto.OrderItemRequestDto;
import com.innowise.orderservice.dto.OrderRequestDto;
import com.innowise.orderservice.dto.OrderResponseDto;
import com.innowise.orderservice.dto.UserDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.entity.Status;
import com.innowise.orderservice.exception.*;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.repository.ItemDao;
import com.innowise.orderservice.repository.OrderDao;
import com.innowise.orderservice.service.OrderService;
import com.innowise.orderservice.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderDao orderDao;
    private final ItemDao itemDao;
    private final OrderMapper  orderMapper;
    private final UserClient userClient;

    @Override
    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto orderRequestDto) {
        if(orderRequestDto == null){
            throw new OrderNullParametrException();
        }

        Long userId = orderRequestDto.getUserId();

        Order order = Order.builder()
                .userId(userId)
                .orderItems(new ArrayList<>())
                .build();


        List<OrderItemRequestDto> orderItemRequestDtos = orderRequestDto.getOrderItems();

        Long totalPrice = calculateTotalPrice(orderItemRequestDtos, order);

        order.setTotalPrice(totalPrice);

        Order savedOrder = orderDao.save(order);

        OrderResponseDto orderResponseDto = orderMapper.toOrderDto(savedOrder);

        enrichWithUser(orderResponseDto, userId);

        return orderResponseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id) {
        if(id == null){
            throw new OrderNullParametrException();
        }

        Order order = orderDao.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        Long userId = order.getUserId();


        OrderResponseDto orderResponseDto = orderMapper.toOrderDto(order);

        enrichWithUser(orderResponseDto, userId);

        return orderResponseDto;
    }

    @Override
    public Page<OrderResponseDto> getOrdersByUserId(Long userId, Pageable pageable) {

        if(pageable == null || userId == null){
            throw new OrderNullParametrException();
        }

        Page<Order> ordersByUserIdPage = orderDao.findAllByUserIdAndDeletedFalse(userId, pageable);

        return ordersByUserIdPage.map(order -> {
            OrderResponseDto dto = orderMapper.toOrderDto(order);
            enrichWithUser(dto, order.getUserId());
            return dto;
        });
    }

    @Override
    public Page<OrderResponseDto> getAllOrders(LocalDate from, LocalDate to, List<Status> statuses, Pageable pageable) {
        if(pageable == null){
            throw new OrderNullParametrException();
        }

        Specification<Order> orderSpecification = Specification.allOf(
                        OrderSpecification.notDeleted(),
                        OrderSpecification.byStatus(statuses),
                        OrderSpecification.byDateRange(from,to));

        Page<Order> ordersPage = orderDao.findAll(orderSpecification, pageable);


        return ordersPage.map(order -> {
            OrderResponseDto dto = orderMapper.toOrderDto(order);
            enrichWithUser(dto, order.getUserId());
            return dto;
        });
    }

    @Override
    @Transactional
    public OrderResponseDto setPaidStatus(Long id) {
        if(id == null){
            throw new OrderNullParametrException();
        }

        Order order = orderDao.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        Status status = order.getStatus();

        if(status == Status.CANCELLED){
            throw new OrderCancelledException();
        }

        Long userId = order.getUserId();

        order.setStatus(Status.PAID);

        OrderResponseDto orderResponseDto = orderMapper.toOrderDto(order);

        enrichWithUser(orderResponseDto, userId);

        return orderResponseDto;
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrder(Long orderId, OrderRequestDto orderRequestDto) {
        if(orderRequestDto == null){
            throw new OrderNullParametrException();
        }

        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if(order.isDeleted()){
            throw new OrderNotFoundException(orderId);
        }

        List<OrderItemRequestDto> orderItemRequestDtos = orderRequestDto.getOrderItems();

        order.getOrderItems().clear();

        Long totalPrice = calculateTotalPrice(orderItemRequestDtos, order);
        order.setTotalPrice(totalPrice);

        Order updatedOrder = orderDao.save(order);

        Long userId = updatedOrder.getUserId();

        OrderResponseDto orderResponseDto = orderMapper.toOrderDto(order);

        enrichWithUser(orderResponseDto, userId);

        return orderResponseDto;
    }

    @Override
    @Transactional
    public boolean softdeleteOrder(Long id) {
        if(id == null){
            throw new OrderNullParametrException();
        }

        Order order = orderDao.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if(order.isDeleted()){
            throw new OrderSoftDeleteException(id);
        }

        order.setDeleted(true);

        return true;
    }

    private void enrichWithUser(OrderResponseDto orderResponseDto, Long userId) {
        UserDto userDto = userClient.getUserById(userId);
        orderResponseDto.setUserDto(userDto);
    }

    private Long calculateTotalPrice(List<OrderItemRequestDto>  orderItemRequestDtos, Order order) {
        Long totalPrice = 0L;
        for(OrderItemRequestDto orderItemRequestDto : orderItemRequestDtos){
            Long itemId = orderItemRequestDto.getItemId();

            Item item = itemDao.findById(itemId)
                    .orElseThrow(() -> new ItemNotFoundException(itemId));

            int quantity = orderItemRequestDto.getQuantity();

            OrderItem orderItem = OrderItem.builder()
                    .item(item)
                    .quantity(quantity)
                    .order(order)
                    .build();

            order.getOrderItems().add(orderItem);

            totalPrice += item.getPrice() * quantity;
        }
        return totalPrice;
    }
}
