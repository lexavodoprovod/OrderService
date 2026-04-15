package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.OrderRequestDto;
import com.innowise.orderservice.dto.OrderResponseDto;
import com.innowise.orderservice.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {

    @Mapping(target = "userDto", ignore = true)
    OrderResponseDto toOrderDto(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    Order toOrder(OrderRequestDto requestOrderDto);
}
