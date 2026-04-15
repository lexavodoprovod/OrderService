package com.innowise.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.orderservice.dto.OrderItemRequestDto;
import com.innowise.orderservice.dto.OrderRequestDto;
import com.innowise.orderservice.dto.UserDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.entity.Status;
import com.innowise.orderservice.repository.ItemDao;
import com.innowise.orderservice.repository.OrderDao;
import com.innowise.orderservice.repository.OrderItemDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderControllerTest extends BaseIT{

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemDao itemDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderItemDao  orderItemDao;

    @BeforeEach
    void setUp() {
        orderDao.deleteAll();
        itemDao.deleteAll();
        orderItemDao.deleteAll();
        wireMock.resetAll();
    }

    @Nested
    @Transactional
    @DisplayName("Add Order Integration Tests")
    class AddOrderTests {

        @Test
        @DisplayName("Should create order with items and return 200")
        void shouldCreateOrderSuccessfully() throws Exception {
            Item item = itemDao.save(Item.builder()
                    .name("Smartphone")
                    .price(50000L)
                    .build());


            Long userId = 1L;
            OrderItemRequestDto itemRequest = new OrderItemRequestDto(item.getId(), 2);
            OrderRequestDto orderRequest = new OrderRequestDto(userId, List.of(itemRequest));

            wireMock.stubFor(get(urlEqualTo("/users/" + userId))
                    .willReturn(okJson(objectMapper.writeValueAsString(new UserDto()))));

            mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(orderRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.totalPrice").value(100000))
                    .andExpect(jsonPath("$.orderItems.length()").value(1));

            List<Order> orders = orderDao.findAll();
            assertEquals(1, orders.size());
            assertEquals(100000L, orders.get(0).getTotalPrice());
            assertEquals(1, orders.get(0).getOrderItems().size());
        }

        @Test
        @DisplayName("Should return 400 when order items are empty")
        void shouldReturnBadRequestWhenNoItems() throws Exception {
            OrderRequestDto emptyOrder = new OrderRequestDto(1L, List.of());

            mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyOrder)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when ordered item does not exist")
        void shouldReturnNotFoundWhenItemMissing() throws Exception {
            OrderItemRequestDto missingItem = new OrderItemRequestDto(999L, 1);
            OrderRequestDto orderRequest = new OrderRequestDto(1L, List.of(missingItem));

            mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(orderRequest)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Get Order By Id Integration Tests")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return order with items and user data - Success")
        void shouldReturnOrderSuccessfully() throws Exception {
            Long userId = 1L;
            Order order = orderDao.save(Order.builder()
                    .userId(userId)
                    .totalPrice(100000L)
                    .status(Status.NEW)
                    .build());

            UserDto mockUser = UserDto.builder().id(userId).name("Nikita").build();

            wireMock.stubFor(WireMock.get(urlEqualTo("/users/" + userId))
                    .willReturn(okJson(objectMapper.writeValueAsString(mockUser))));


            mockMvc.perform(MockMvcRequestBuilders.get("/orders/{id}", order.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(order.getId()));
        }

        @Test
        @DisplayName("Should return 404 when order is not found in database")
        void shouldReturn404WhenNotFound() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/orders/{id}", 999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when ID is not a number")
        void shouldReturn400WhenIdTypeIsInvalid() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/orders/invalid-id"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle Feign Client failure (User Service Down)")
        void shouldHandleUserServiceFailure() throws Exception {
            Long userId = 1L;
            Order order = orderDao.save(Order.builder()
                    .userId(userId)
                    .totalPrice(0L)
                    .build());

            wireMock.stubFor(WireMock.get(urlEqualTo("/users/" +  userId))
                    .willReturn(serviceUnavailable()));

            mockMvc.perform(MockMvcRequestBuilders.get("/orders/{id}", order.getId()))
                    .andExpect(MockMvcResultMatchers.status().isServiceUnavailable());
        }

        @Test
        @DisplayName("Should return order even if it's marked as deleted (if business logic allows)")
        void shouldReturnDeletedOrder() throws Exception {
            Long userId = 1L;
            Order deletedOrder = orderDao.save(Order.builder()
                    .userId(userId)
                    .totalPrice(10L)
                    .deleted(true)
                    .build());

            wireMock.stubFor(WireMock.get(urlEqualTo("/users/" + userId))
                    .willReturn(okJson(objectMapper.writeValueAsString(new UserDto()))));

            mockMvc.perform(MockMvcRequestBuilders.get("/orders/{id}", deletedOrder.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deleted").value(true));
        }
    }

    @Nested
    @DisplayName("Get Orders By Id Integration Tests")
    class GetOrdersByUserId{

        @Test
        @DisplayName("Should return all orders by user id")
        void shouldReturnAllOrdersByUserId() throws Exception {
            Long firstUserId = 1L;
            Long secondUserId = 2L;

            saveOrder(firstUserId, Status.NEW, LocalDate.now());
            saveOrder(firstUserId, Status.PAID, LocalDate.now());
            saveOrder(secondUserId, Status.NEW, LocalDate.now());


            wireMock.stubFor(get(urlEqualTo("/users/" + firstUserId))
                    .willReturn(okJson(objectMapper.writeValueAsString(new UserDto()))));

            mockMvc.perform(MockMvcRequestBuilders.get("/orders/user/{id}", firstUserId))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(2))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(2));
        }
    }

    @Nested
    @DisplayName("Get All Orders Integration Tests")
    class GetAllOrdersTests {

        @Test
        @DisplayName("Should return all orders with default pagination")
        void shouldReturnAllOrdersSuccessfully() throws Exception {
            saveOrder(10L, Status.NEW, LocalDate.now());
            saveOrder(20L, Status.PAID, LocalDate.now());

            wireMock.stubFor(WireMock.get(urlMatching("/users/[0-9]+"))
                    .willReturn(okJson(objectMapper.writeValueAsString(new UserDto()))));

            mockMvc.perform(MockMvcRequestBuilders.get("/orders"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(2))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("Should filter orders by date range using current time")
        void shouldFilterByDateRange() throws Exception {
            Long userId = 1L;
            saveOrder(userId, Status.NEW, LocalDate.now());

            String from = LocalDate.now().minusDays(1).toString();
            String to = LocalDate.now().plusDays(1).toString();

            wireMock.stubFor(WireMock.get(urlEqualTo("/users/" +  userId))
                    .willReturn(okJson(objectMapper.writeValueAsString(new UserDto()))));

            mockMvc.perform(MockMvcRequestBuilders.get("/orders")
                            .param("from", from)
                            .param("to", to))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(1));
        }

        @Test
        @DisplayName("Should filter orders by multiple statuses")
        void shouldFilterByStatuses() throws Exception {
            Long userId = 1L;

            saveOrder(userId, Status.NEW, LocalDate.now());
            saveOrder(userId, Status.PAID, LocalDate.now());
            saveOrder(userId, Status.CANCELLED, LocalDate.now());

            wireMock.stubFor(WireMock.get(urlEqualTo("/users/" + userId))
                    .willReturn(okJson(objectMapper.writeValueAsString(new UserDto()))));

            mockMvc.perform(MockMvcRequestBuilders.get("/orders")
                            .param("statuses", "NEW,PAID"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(2));
        }

        @Test
        @DisplayName("Should not return soft-deleted orders")
        void shouldHideDeletedOrders() throws Exception {
            Long userId = 1L;
            Order active = Order.builder().userId(userId).totalPrice(100L).deleted(false).build();
            Order deleted = Order.builder().userId(userId).totalPrice(200L).deleted(true).build();
            orderDao.saveAll(List.of(active, deleted));

            wireMock.stubFor(WireMock.get(urlEqualTo("/users/" + userId))
                    .willReturn(okJson(objectMapper.writeValueAsString(new UserDto()))));

            mockMvc.perform(MockMvcRequestBuilders.get("/orders"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(1))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].deleted").value(false));
        }

        @Test
        @DisplayName("Should return 400 when date format is invalid")
        void shouldReturnBadRequestForInvalidDate() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/orders")
                            .param("from", "01-01-2026"))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }


    }

    @Nested
    @DisplayName("Update Order Integration Tests")
    class UpdateOrderTests {

        @Test
        @DisplayName("Should successfully update order items and total price")
        @Transactional
        void shouldUpdateOrderSuccessfully() throws Exception {
            Long userId = 1L;
            Item laptop = itemDao.save(Item.builder().name("Laptop").price(100000L).build());
            Item mouse = itemDao.save(Item.builder().name("Mouse").price(2000L).build());

            Order order = Order.builder().userId(userId).status(Status.NEW).totalPrice(100000L).build();
            order = orderDao.save(order);
            orderItemDao.save(OrderItem.builder().order(order).item(laptop).quantity(1).build());

            OrderItemRequestDto updateItem = new OrderItemRequestDto(mouse.getId(), 2);
            OrderRequestDto updateRequest = new OrderRequestDto(userId, List.of(updateItem));

            UserDto userDto = UserDto.builder().id(userId).name("Nikita").build();

            wireMock.stubFor(WireMock.get(urlEqualTo("/users/" + userId))
                    .willReturn(okJson(objectMapper.writeValueAsString(userDto))));

            mockMvc.perform(MockMvcRequestBuilders.put("/orders/{id}", order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalPrice").value(4000))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.orderItems.length()").value(1))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.orderItems[0].itemDto.name").value("Mouse"));

            Order updatedOrder = orderDao.findById(order.getId()).orElseThrow();
            assertEquals(4000L, updatedOrder.getTotalPrice());
            assertEquals(1, updatedOrder.getOrderItems().size());
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent order")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            OrderRequestDto request = new OrderRequestDto(1L, List.of(new OrderItemRequestDto(1L, 1)));

            mockMvc.perform(MockMvcRequestBuilders.put("/orders/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when order items list is empty")
        void shouldReturn400WhenValidationFails() throws Exception {
            OrderRequestDto invalidRequest = new OrderRequestDto(1L, List.of());

            mockMvc.perform(MockMvcRequestBuilders.put("/orders/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when adding non-existent item to order")
        void shouldReturn404WhenItemInRequestNotFound() throws Exception {
            Order order = orderDao.save(Order.builder().userId(1L).totalPrice(0L).build());

            OrderRequestDto requestWithGhostItem = new OrderRequestDto(1L, List.of(new OrderItemRequestDto(999L, 1)));

            mockMvc.perform(MockMvcRequestBuilders.put("/orders/{id}", order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithGhostItem)))
                    .andExpect(MockMvcResultMatchers.status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Set Paid Status Integration Tests")
    class SetPaidStatusTests {

        @Test
        @DisplayName("Should successfully change status to PAID and return 200")
        void shouldSetStatusToPaidSuccessfully() throws Exception {
            Long userId = 1L;
            Order order = orderDao.save(Order.builder()
                    .userId(userId)
                    .status(Status.NEW)
                    .totalPrice(5000L)
                    .build());

            UserDto userDto = UserDto.builder().id(userId).name("Nikita").build();

            wireMock.stubFor(WireMock.get(urlEqualTo("/users/" + userId))
                    .willReturn(okJson(objectMapper.writeValueAsString(userDto))));

            mockMvc.perform(MockMvcRequestBuilders.patch("/orders/{id}/paid", order.getId()))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("PAID"));

            Order updatedOrder = orderDao.findById(order.getId()).orElseThrow();
            assertEquals(Status.PAID, updatedOrder.getStatus());
        }

        @Test
        @DisplayName("Should return 404 when trying to pay for non-existent order")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.patch("/orders/{id}/paid", 999L))
                    .andExpect(MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when order is already CANCELLED")
        void shouldReturn400WhenOrderIsCancelled() throws Exception {
            Long userId = 1L;
            Order cancelledOrder = orderDao.save(Order.builder()
                    .userId(userId)
                    .status(Status.CANCELLED)
                    .totalPrice(100L)
                    .build());

            mockMvc.perform(MockMvcRequestBuilders.patch("/orders/{id}/paid", cancelledOrder.getId()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when ID format is invalid")
        void shouldReturn400WhenIdIsInvalid() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.patch("/orders/abc/paid"))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Delete Order Integration Tests (Soft Delete)")
    class DeleteOrderTests {

        @Test
        @DisplayName("Should set deleted flag to true and return 204 No Content")
        void shouldSoftDeleteOrderSuccessfully() throws Exception {
            Order order = Order.builder()
                    .userId(1L)
                    .status(Status.NEW)
                    .totalPrice(1500L)
                    .deleted(false)
                    .build();
            order = orderDao.save(order);
            Long id = order.getId();

            mockMvc.perform(MockMvcRequestBuilders.delete("/orders/{id}", id))
                    .andExpect(MockMvcResultMatchers.status().isNoContent());

            Optional<Order> deletedOrder = orderDao.findById(id);
            assertTrue(deletedOrder.isPresent(), "Order should still exist in DB (Soft Delete)");
            assertTrue(deletedOrder.get().isDeleted(), "Order flag 'deleted' should be true");
        }

        @Test
        @DisplayName("Should return 404 when trying to delete non-existent order")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete("/orders/{id}", 999L))
                    .andExpect(MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when trying to delete already deleted order")
        void shouldReturn404WhenOrderAlreadyDeleted() throws Exception {
            Order alreadyDeleted = orderDao.save(Order.builder()
                    .userId(1L)
                    .deleted(true)
                    .build());

            mockMvc.perform(MockMvcRequestBuilders.delete("/orders/{id}", alreadyDeleted.getId()))
                    .andExpect(MockMvcResultMatchers.status().isConflict());
        }

        @Test
        @DisplayName("Should return 400 when ID format is invalid")
        void shouldReturn400WhenIdIsInvalid() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete("/orders/not-a-number"))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }
    }

    private void saveOrder(Long userId, Status status, LocalDate date) {
        Order order = Order.builder()
                .userId(userId)
                .status(status)
                .totalPrice(1000L)
                .build();
        order.setCreatedAt(date.atStartOfDay());
        orderDao.save(order);
    }

}