package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.OrderItemResponseDto;
import com.innowise.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {ItemMapper.class})
public interface OrderItemMapper {
    OrderItem toOrderItem(OrderItemResponseDto orderItemDto);
    OrderItemResponseDto toOrderItemDto(OrderItem orderItem);
}
