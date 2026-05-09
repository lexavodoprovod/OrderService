package com.innowise.orderservice.dto.kafka;


import com.innowise.orderservice.entity.EventType;
import com.innowise.orderservice.entity.PaymentStatus;

public record PaymentEventDto(
        EventType eventType,
        String paymentId,
        Long orderId,
        PaymentStatus status
) {}
