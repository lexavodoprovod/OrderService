package com.innowise.orderservice.dto.kafka;


import com.innowise.orderservice.entity.PaymentStatus;

public record PaymentEventDto(
        Long orderId,
        PaymentStatus status
) {}
