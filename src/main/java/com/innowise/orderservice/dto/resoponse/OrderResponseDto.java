package com.innowise.orderservice.dto.resoponse;

import com.innowise.orderservice.dto.UserDto;
import com.innowise.orderservice.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDto {

    private Long id;

    private UserDto userDto;

    private OrderStatus status;

    private Long totalPrice;

    private boolean deleted;

    private List<OrderItemResponseDto> orderItems;
}
