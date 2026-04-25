package com.innowise.orderservice.consumer;

import com.innowise.orderservice.controller.BaseIT;
import com.innowise.orderservice.dto.kafka.PaymentEventDto;
import com.innowise.orderservice.entity.PaymentStatus;
import com.innowise.orderservice.service.impl.OrderServiceImpl;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;



import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;


class PaymentKafkaConsumerTest extends  BaseIT{

    @Autowired
    private KafkaTemplate<String, PaymentEventDto> kafkaTemplate;

    @MockitoBean
    private OrderServiceImpl orderService;

    @Value("${topic-name.status}")
    private String topicName;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @TestConfiguration
    static class KafkaTestConfig {
        @Bean
        public NewTopic paymentStatusTopic(@Value("${topic-name.status}") String name) {
            return TopicBuilder.name(name)
                    .partitions(1)
                    .replicas(1)
                    .build();
        }
    }


    @BeforeEach
    void waitForKafka() {
        registry.getListenerContainers().forEach(container -> {
            await()
                    .atMost(30, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofSeconds(1))
                    .atMost(Duration.ofSeconds(30))
                    .untilAsserted(() -> {
                        ContainerTestUtils.waitForAssignment(container, 1);
                    });
        });
    }


    @Nested
    @DisplayName("Consume Payment Event Tests")
    class ConsumePaymentEventTests {

        @Test
        @DisplayName("Should consume SUCCESS payment event")
        void shouldConsumeSuccessEvent() {
            PaymentEventDto event = new PaymentEventDto(1L, PaymentStatus.SUCCESS);

            kafkaTemplate.send(topicName, event);


            await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(1000)).untilAsserted(() -> {
                System.out.println("Verified consumption of SUCCESS event");
                verify(orderService).setPaidStatus(event.orderId());
            });
        }

        @Test
        @DisplayName("Should consume FAILED payment event")
        void shouldConsumeFailedEvent() {
            PaymentEventDto event = new PaymentEventDto(2L, PaymentStatus.FAILED);

            kafkaTemplate.send(topicName, event);

            await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(1000)).untilAsserted(() -> {
                System.out.println("Verified consumption of FAILED event");
                verify(orderService).setCancelledStatus(event.orderId());
            });
        }


    }
}