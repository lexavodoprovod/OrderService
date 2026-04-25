package com.innowise.orderservice.consumer;

import com.innowise.orderservice.dto.kafka.PaymentEventDto;
import com.innowise.orderservice.entity.PaymentStatus;
import com.innowise.orderservice.service.impl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PaymentKafkaConsumer {

    private final OrderServiceImpl orderService;

    @KafkaListener(
            topics = "${topic-name.status}",
            groupId = "order-consumer-1",
            containerFactory = "kafkaListenerFactory"
    )
    public void consumePaymentEvent(PaymentEventDto paymentEventDto) {
        System.out.println("Received payment event for order %s: %s"
                .formatted(paymentEventDto.orderId(), paymentEventDto.status()));

        Long orderId = paymentEventDto.orderId();
        PaymentStatus paymentStatus = paymentEventDto.status();

        if(PaymentStatus.SUCCESS.equals(paymentStatus)) {
            System.out.println("Should change order status to PAID");
            orderService.setPaidStatus(orderId);
        }else{
            System.out.println("Should change order status to CANCELLED");
            orderService.setCancelledStatus(orderId);
        }
    }
}
