package com.innowise.orderservice.dto.request;

import com.innowise.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdateStatusRequest {
    @NotNull
    private OrderStatus orderStatus;
}
