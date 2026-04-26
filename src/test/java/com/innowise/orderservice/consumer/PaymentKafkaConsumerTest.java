package com.innowise.orderservice.consumer;

import com.innowise.orderservice.controller.BaseIT;
import com.innowise.orderservice.dto.kafka.PaymentEventDto;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.entity.PaymentStatus;
import com.innowise.orderservice.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;



import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import java.time.Duration;
import java.util.concurrent.ExecutionException;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@Slf4j
class PaymentKafkaConsumerTest extends  BaseIT{

    @Autowired
    private KafkaTemplate<String, PaymentEventDto> kafkaTemplate;

    @MockitoBean
    private OrderService orderService;

    @Value("${topic-name.status}")
    private String topicName;

    @Nested
    @DisplayName("Consume Payment Event Tests")
    class ConsumePaymentEventTests {

        @Test
        @DisplayName("Should consume SUCCESS payment event")
        void shouldConsumeSuccessEvent() throws ExecutionException, InterruptedException {
            PaymentEventDto event = new PaymentEventDto(1L, PaymentStatus.SUCCESS);
            Long orderId = event.orderId();

            kafkaTemplate.send(topicName, event).get();


            await().atMost(Duration.ofSeconds(30))
                    .pollDelay(Duration.ofSeconds(1))
                    .pollInterval(Duration.ofMillis(1000))
                    .untilAsserted(() -> {
                        log.info("Trying to verified consumption of SUCCESS event");
                        verify(orderService, atLeastOnce()).updateStatus(orderId, OrderStatus.PAID);
                        log.info("Verified consumption of SUCCESS event");
                    });
        }

        @Test
        @DisplayName("Should consume FAILED payment event")
        void shouldConsumeFailedEvent() throws ExecutionException, InterruptedException {
            PaymentEventDto event = new PaymentEventDto(2L, PaymentStatus.FAILED);
            Long orderId = event.orderId();

            kafkaTemplate.send(topicName, event).get();

            await().atMost(Duration.ofSeconds(30))
                    .pollDelay(Duration.ofSeconds(1))
                    .pollInterval(Duration.ofMillis(1000))
                    .untilAsserted(() -> {
                        log.info("Trying to verified consumption of FAILED event");
                        verify(orderService, atLeastOnce()).updateStatus(orderId, OrderStatus.CANCELLED);
                        log.info("Verified consumption of FAILED event");
            });
        }


    }
}