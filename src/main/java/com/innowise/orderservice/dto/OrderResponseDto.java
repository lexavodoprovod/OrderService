package com.innowise.orderservice.dto;

import com.innowise.orderservice.entity.Status;
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

    private Status status;

    private Long totalPrice;

    private boolean deleted;

    private List<OrderItemResponseDto> orderItems;
}
