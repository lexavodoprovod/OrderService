package com.innowise.orderservice.dto.resoponse;

import com.innowise.orderservice.dto.ItemDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponseDto {
    private Long id;

    private ItemDto itemDto;

    private int quantity;
}
