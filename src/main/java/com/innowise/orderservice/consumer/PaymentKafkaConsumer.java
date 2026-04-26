package com.innowise.orderservice.consumer;

import com.innowise.orderservice.dto.kafka.PaymentEventDto;
import com.innowise.orderservice.entity.PaymentStatus;
import com.innowise.orderservice.service.impl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class PaymentKafkaConsumer {

    private final OrderServiceImpl orderService;

    @KafkaListener(
            topics = "${topic-name.status}",
            groupId = "order-consumer-1",
            containerFactory = "kafkaListenerFactory"
    )
    public void consumePaymentEvent(PaymentEventDto paymentEventDto) {
        log.info("Received payment event for order {}: {}", paymentEventDto.orderId(), paymentEventDto.status());

        Long orderId = paymentEventDto.orderId();
        PaymentStatus paymentStatus = paymentEventDto.status();

        if(PaymentStatus.SUCCESS.equals(paymentStatus)) {
            log.info("Order status changed to PAID");
            orderService.setPaidStatus(orderId);
        }else{
            log.info("Order status changed to CANCELLED");
            orderService.setCancelledStatus(orderId);
        }
    }
}
