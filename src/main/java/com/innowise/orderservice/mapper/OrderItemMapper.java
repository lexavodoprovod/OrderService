package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.resoponse.OrderItemResponseDto;
import com.innowise.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ItemMapper.class})
public interface OrderItemMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(source = "itemDto", target = "item")
    OrderItem toOrderItem(OrderItemResponseDto orderItemDto);

    @Mapping(source = "item", target = "itemDto")
    OrderItemResponseDto toOrderItemDto(OrderItem orderItem);
}
