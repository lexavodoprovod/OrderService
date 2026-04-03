package com.innowise.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.client.UserClient;
import com.innowise.orderservice.dto.OrderItemRequestDto;
import com.innowise.orderservice.dto.OrderRequestDto;
import com.innowise.orderservice.dto.UserDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.entity.Status;
import com.innowise.orderservice.exception.UserServiceException;
import com.innowise.orderservice.repository.ItemDao;
import com.innowise.orderservice.repository.OrderDao;
import com.innowise.orderservice.repository.OrderItemDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;


class OrderControllerTest extends BaseIT{

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserClient userClient;

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

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/orders")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(orderRequest)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalPrice").value(100000))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.orderItems.length()").value(1));

            List<Order> orders = orderDao.findAll();
            assertEquals(1, orders.size());
            assertEquals(100000L, orders.get(0).getTotalPrice());
            assertEquals(1, orders.get(0).getOrderItems().size());
        }

        @Test
        @DisplayName("Should return 400 when order items are empty")
        void shouldReturnBadRequestWhenNoItems() throws Exception {
            OrderRequestDto emptyOrder = new OrderRequestDto(1L, List.of());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/orders")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyOrder)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when ordered item does not exist")
        void shouldReturnNotFoundWhenItemMissing() throws Exception {
            OrderItemRequestDto missingItem = new OrderItemRequestDto(999L, 1);
            OrderRequestDto orderRequest = new OrderRequestDto(1L, List.of(missingItem));

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/orders")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(orderRequest)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Get Order By Id Integration Tests")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return order with items and user data - Success")
        void shouldReturnOrderSuccessfully() throws Exception {
            Item item = itemDao.save(Item.builder().name("Laptop").price(100000L).build());
            Order order = orderDao.save(Order.builder()
                    .userId(1L)
                    .totalPrice(100000L)
                    .status(Status.NEW)
                    .build());

            UserDto mockUser = UserDto.builder().id(1L).name("Nikita").build();
            when(userClient.getUserById(1L)).thenReturn(mockUser);

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/orders/{id}", order.getId()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value(order.getId()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.orderItems[0].itemDto.name").value("Laptop"));
        }

        @Test
        @DisplayName("Should return 404 when order is not found in database")
        void shouldReturn404WhenNotFound() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/orders/{id}", 999L))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when ID is not a number")
        void shouldReturn400WhenIdTypeIsInvalid() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/orders/invalid-id"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle Feign Client failure (User Service Down)")
        void shouldHandleUserServiceFailure() throws Exception {
            Order order = orderDao.save(Order.builder().userId(1L).totalPrice(0L).build());

            when(userClient.getUserById(1L)).thenThrow(new UserServiceException());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/orders/{id}", order.getId()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isServiceUnavailable());
        }

        @Test
        @DisplayName("Should return order even if it's marked as deleted (if business logic allows)")
        void shouldReturnDeletedOrder() throws Exception {
            Order deletedOrder = orderDao.save(Order.builder()
                    .userId(1L)
                    .totalPrice(10L)
                    .deleted(true)
                    .build());

            when(userClient.getUserById(1L)).thenReturn(new UserDto());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/orders/{id}", deletedOrder.getId()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.deleted").value(true));
        }
    }

    @Nested
    @DisplayName("Get All Orders Integration Tests")
    class GetAllOrdersTests {

        @BeforeEach
        void setUp() {
            orderDao.deleteAll();
            itemDao.deleteAll();
        }

        @Test
        @DisplayName("Should return all orders with default pagination")
        void shouldReturnAllOrdersSuccessfully() throws Exception {
            saveOrder(10L, Status.NEW, LocalDate.now());
            saveOrder(20L, Status.PAID, LocalDate.now());

            when(userClient.getUserById(anyLong())).thenReturn(new UserDto());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/orders"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content.length()").value(2))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("Should filter orders by date range using current time")
        void shouldFilterByDateRange() throws Exception {
            saveOrder(1L, Status.NEW, LocalDate.now());

            String from = LocalDate.now().minusDays(1).toString();
            String to = LocalDate.now().plusDays(1).toString();

            when(userClient.getUserById(anyLong())).thenReturn(new UserDto());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/orders")
                            .param("from", from)
                            .param("to", to))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content.length()").value(1));
        }

        @Test
        @DisplayName("Should filter orders by multiple statuses")
        void shouldFilterByStatuses() throws Exception {
            saveOrder(1L, Status.NEW, LocalDate.now());
            saveOrder(1L, Status.PAID, LocalDate.now());
            saveOrder(1L, Status.CANCELLED, LocalDate.now());

            when(userClient.getUserById(anyLong())).thenReturn(new UserDto());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/orders")
                            .param("statuses", "NEW,PAID"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content.length()").value(2));
        }

        @Test
        @DisplayName("Should not return soft-deleted orders")
        void shouldHideDeletedOrders() throws Exception {
            Order active = Order.builder().userId(1L).totalPrice(100L).deleted(false).build();
            Order deleted = Order.builder().userId(1L).totalPrice(200L).deleted(true).build();
            orderDao.saveAll(List.of(active, deleted));

            when(userClient.getUserById(anyLong())).thenReturn(new UserDto());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/orders"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content.length()").value(1))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content[0].deleted").value(false));
        }

        @Test
        @DisplayName("Should return 400 when date format is invalid")
        void shouldReturnBadRequestForInvalidDate() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/orders")
                            .param("from", "01-01-2026"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
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

    @Nested
    @DisplayName("Update Order Integration Tests")
    class UpdateOrderTests {

        @Test
        @DisplayName("Should successfully update order items and total price")
        @Transactional
        void shouldUpdateOrderSuccessfully() throws Exception {
            Item laptop = itemDao.save(Item.builder().name("Laptop").price(100000L).build());
            Item mouse = itemDao.save(Item.builder().name("Mouse").price(2000L).build());

            Order order = Order.builder().userId(1L).status(Status.NEW).totalPrice(100000L).build();
            order = orderDao.save(order);
            orderItemDao.save(OrderItem.builder().order(order).item(laptop).quantity(1).build());

            OrderItemRequestDto updateItem = new OrderItemRequestDto(mouse.getId(), 2);
            OrderRequestDto updateRequest = new OrderRequestDto(1L, List.of(updateItem));

            when(userClient.getUserById(1L)).thenReturn(UserDto.builder().id(1L).name("Nikita").build());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/orders/{id}", order.getId())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalPrice").value(4000))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.orderItems.length()").value(1))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.orderItems[0].itemDto.name").value("Mouse"));

            Order updatedOrder = orderDao.findById(order.getId()).orElseThrow();
            assertEquals(4000L, updatedOrder.getTotalPrice());
            assertEquals(1, updatedOrder.getOrderItems().size());
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent order")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            OrderRequestDto request = new OrderRequestDto(1L, List.of(new OrderItemRequestDto(1L, 1)));

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/orders/{id}", 999L)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when order items list is empty")
        void shouldReturn400WhenValidationFails() throws Exception {
            OrderRequestDto invalidRequest = new OrderRequestDto(1L, List.of());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/orders/{id}", 1L)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when adding non-existent item to order")
        void shouldReturn404WhenItemInRequestNotFound() throws Exception {
            Order order = orderDao.save(Order.builder().userId(1L).totalPrice(0L).build());

            OrderRequestDto requestWithGhostItem = new OrderRequestDto(1L, List.of(new OrderItemRequestDto(999L, 1)));

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/orders/{id}", order.getId())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithGhostItem)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Set Paid Status Integration Tests")
    class SetPaidStatusTests {

        @Test
        @DisplayName("Should successfully change status to PAID and return 200")
        void shouldSetStatusToPaidSuccessfully() throws Exception {
            Order order = orderDao.save(Order.builder()
                    .userId(1L)
                    .status(Status.NEW)
                    .totalPrice(5000L)
                    .build());

            when(userClient.getUserById(1L)).thenReturn(UserDto.builder().id(1L).name("Nikita").build());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/orders/{id}/paid", order.getId()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("PAID"));

            Order updatedOrder = orderDao.findById(order.getId()).orElseThrow();
            assertEquals(Status.PAID, updatedOrder.getStatus());
        }

        @Test
        @DisplayName("Should return 404 when trying to pay for non-existent order")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/orders/{id}/paid", 999L))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when order is already CANCELLED")
        void shouldReturn400WhenOrderIsCancelled() throws Exception {
            Order cancelledOrder = orderDao.save(Order.builder()
                    .userId(1L)
                    .status(Status.CANCELLED)
                    .totalPrice(100L)
                    .build());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/orders/{id}/paid", cancelledOrder.getId()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when ID format is invalid")
        void shouldReturn400WhenIdIsInvalid() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/orders/abc/paid"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
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

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/orders/{id}", id))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());

            Optional<Order> deletedOrder = orderDao.findById(id);
            assertTrue(deletedOrder.isPresent(), "Order should still exist in DB (Soft Delete)");
            assertTrue(deletedOrder.get().isDeleted(), "Order flag 'deleted' should be true");
        }

        @Test
        @DisplayName("Should return 404 when trying to delete non-existent order")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/orders/{id}", 999L))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when trying to delete already deleted order")
        void shouldReturn404WhenOrderAlreadyDeleted() throws Exception {
            Order alreadyDeleted = orderDao.save(Order.builder()
                    .userId(1L)
                    .deleted(true)
                    .build());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/orders/{id}", alreadyDeleted.getId()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when ID format is invalid")
        void shouldReturn400WhenIdIsInvalid() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/orders/not-a-number"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        }
    }

}