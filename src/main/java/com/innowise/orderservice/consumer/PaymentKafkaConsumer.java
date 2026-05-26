package com.innowise.orderservice.consumer;

import com.innowise.orderservice.dto.kafka.PaymentEventDto;
import com.innowise.orderservice.entity.EventType;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.entity.PaymentStatus;
import com.innowise.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class PaymentKafkaConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${topic-name.status}",
            groupId = "order-consumer-group",
            containerFactory = "kafkaListenerFactory"
    )
    public void consumePaymentEvent(PaymentEventDto paymentEventDto) {
        log.info("Received payment event for order {}: {}", paymentEventDto.orderId(), paymentEventDto.status());

        Long orderId = paymentEventDto.orderId();
        PaymentStatus paymentStatus = paymentEventDto.status();
        EventType eventType = paymentEventDto.eventType();
        switch (eventType){
            case CREATE_PAYMENT:
                if(PaymentStatus.SUCCESS.equals(paymentStatus)) {
                    orderService.updateStatus(orderId, OrderStatus.PAID);
                    log.info("Order status changed to PAID");
                }else{
                    orderService.updateStatus(orderId, OrderStatus.CANCELLED);
                    log.info("Order status changed to CANCELLED");
                }
                break;
            default:
                log.error("Unsupported payment event type");
        }

    }
}
