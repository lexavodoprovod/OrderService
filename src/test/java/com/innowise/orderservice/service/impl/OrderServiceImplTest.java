package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.client.UserClient;
import com.innowise.orderservice.dto.OrderItemRequestDto;
import com.innowise.orderservice.dto.OrderRequestDto;
import com.innowise.orderservice.dto.OrderResponseDto;
import com.innowise.orderservice.dto.UserDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.entity.Status;
import com.innowise.orderservice.exception.ItemNotFoundException;
import com.innowise.orderservice.exception.OrderCancelledException;
import com.innowise.orderservice.exception.OrderNotFoundException;
import com.innowise.orderservice.exception.OrderNullParametrException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.repository.ItemDao;
import com.innowise.orderservice.repository.OrderDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private ItemDao itemDao;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully")
        void shouldCreateOrderSuccessfully() {
            Long userId = 1L;
            Long itemId = 10L;
            Long price = 500L;
            int quantity = 2;

            OrderItemRequestDto itemRequest = new OrderItemRequestDto(itemId, quantity);
            OrderRequestDto orderRequest = new OrderRequestDto(userId, List.of(itemRequest));

            Item item = Item.builder().id(itemId).name("Drill").price(price).build();
            UserDto userDto = UserDto.builder().id(userId).name("Nikita").build();

            Order savedOrder = Order.builder().id(100L).userId(userId).totalPrice(1000L).build();
            OrderResponseDto responseDto = new OrderResponseDto();
            responseDto.setId(100L);

            when(itemDao.findById(itemId)).thenReturn(Optional.of(item));
            when(orderDao.save(any(Order.class))).thenReturn(savedOrder);
            when(orderMapper.toOrderDto(any(Order.class))).thenReturn(responseDto);
            when(userClient.getUserById(userId)).thenReturn(userDto);

            OrderResponseDto result = orderService.createOrder(orderRequest);

            assertNotNull(result);
            assertEquals(userDto, result.getUserDto());

            verify(itemDao).findById(itemId);
            verify(orderDao).save(any(Order.class));
            verify(userClient).getUserById(userId);
        }

        @Test
        @DisplayName("Should throw OrderNullParametrException when dto is null")
        void shouldThrowExceptionWhenRequestIsNull() {
            assertThrows(OrderNullParametrException.class, () -> orderService.createOrder(null));
            verifyNoInteractions(orderDao, itemDao, userClient);
        }

        @Test
        @DisplayName("Should throw ItemNotFoundException when one of items does not exist")
        void shouldThrowExceptionWhenItemNotFound() {
            Long userId = 1L;
            Long missingItemId = 99L;
            OrderItemRequestDto itemRequest = new OrderItemRequestDto(missingItemId, 1);
            OrderRequestDto orderRequest = new OrderRequestDto(userId, List.of(itemRequest));

            when(itemDao.findById(missingItemId)).thenReturn(Optional.empty());

            assertThrows(ItemNotFoundException.class, () -> orderService.createOrder(orderRequest));

            verify(itemDao).findById(missingItemId);
            verify(orderDao, never()).save(any());
            verifyNoInteractions(userClient);
        }

        @Test
        @DisplayName("Should correctly calculate total price for multiple items")
        void shouldCalculateTotalForMultipleItems() {
            OrderItemRequestDto req1 = new OrderItemRequestDto(1L, 2);
            OrderItemRequestDto req2 = new OrderItemRequestDto(2L, 1);
            OrderRequestDto orderRequest = new OrderRequestDto(1L, List.of(req1, req2));

            Item item1 = Item.builder().id(1L).price(100L).build();
            Item item2 = Item.builder().id(2L).price(500L).build();

            when(itemDao.findById(1L)).thenReturn(Optional.of(item1));
            when(itemDao.findById(2L)).thenReturn(Optional.of(item2));
            when(orderDao.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
            when(orderMapper.toOrderDto(any())).thenReturn(new OrderResponseDto());

            orderService.createOrder(orderRequest);


            verify(orderDao).save(argThat(order -> order.getTotalPrice() == 700L));
        }

        @Test
        @DisplayName("Should handle null user info from Feign client gracefully")
        void shouldHandleNullUserFromClient() {
            OrderRequestDto request = new OrderRequestDto(1L, List.of(new OrderItemRequestDto(1L, 1)));
            when(itemDao.findById(any())).thenReturn(Optional.of(Item.builder().price(10L).build()));
            when(orderDao.save(any())).thenReturn(new Order());
            when(orderMapper.toOrderDto(any())).thenReturn(new OrderResponseDto());

            when(userClient.getUserById(1L)).thenReturn(null);

            OrderResponseDto result = orderService.createOrder(request);

            assertNull(result.getUserDto());
            verify(userClient).getUserById(1L);
        }
    }

    @Nested
    @DisplayName("Get Order By Id Tests")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return OrderResponseDto when order exists")
        void shouldReturnOrderSuccessfully() {
            Long orderId = 1L;
            Long userId = 10L;
            Order order = Order.builder().id(orderId).userId(userId).build();
            OrderResponseDto responseDto = new OrderResponseDto();
            UserDto userDto = UserDto.builder().id(userId).name("Nikita").build();

            when(orderDao.findById(orderId)).thenReturn(Optional.of(order));
            when(orderMapper.toOrderDto(order)).thenReturn(responseDto);
            when(userClient.getUserById(userId)).thenReturn(userDto);

            OrderResponseDto result = orderService.getOrderById(orderId);

            assertNotNull(result);
            assertEquals(userDto, result.getUserDto());
            verify(orderDao).findById(orderId);
            verify(userClient).getUserById(userId);
        }

        @Test
        @DisplayName("Should throw OrderNullParametrException when id is null")
        void shouldThrowExceptionWhenIdIsNull() {
            assertThrows(OrderNullParametrException.class, () -> orderService.getOrderById(null));

            verifyNoInteractions(orderDao, orderMapper, userClient);
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order does not exist")
        void shouldThrowExceptionWhenOrderNotFound() {
            Long orderId = 1L;
            when(orderDao.findById(orderId)).thenReturn(Optional.empty());

            assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(orderId));

            verify(orderDao).findById(orderId);
            verifyNoInteractions(orderMapper, userClient);
        }

        @Test
        @DisplayName("Should handle Feign client failure during enrichment")
        void shouldHandleUserClientFailure() {
            Long orderId = 1L;
            Long userId = 10L;
            Order order = Order.builder().id(orderId).userId(userId).build();
            OrderResponseDto responseDto = new OrderResponseDto();

            when(orderDao.findById(orderId)).thenReturn(Optional.of(order));
            when(orderMapper.toOrderDto(order)).thenReturn(responseDto);
            when(userClient.getUserById(userId)).thenThrow(new RuntimeException("Service down"));


            assertThrows(RuntimeException.class, () -> orderService.getOrderById(orderId));
        }
    }

    @Nested
    @DisplayName("Get All Orders Tests")
    class GetAllOrdersTests {

        @Test
        @DisplayName("Should return page of OrderResponseDto with enriched user data")
        void shouldReturnPageOfOrdersSuccessfully() {
            Pageable pageable = PageRequest.of(0, 10);
            Order order1 = Order.builder().id(1L).userId(10L).build();
            Order order2 = Order.builder().id(2L).userId(20L).build();

            Page<Order> orderPage = new PageImpl<>(List.of(order1, order2), pageable, 2);

            UserDto user1 = UserDto.builder().id(10L).name("User1").build();
            UserDto user2 = UserDto.builder().id(20L).name("User2").build();

            when(orderDao.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);

            when(orderMapper.toOrderDto(order1)).thenReturn(new OrderResponseDto());
            when(orderMapper.toOrderDto(order2)).thenReturn(new OrderResponseDto());

            when(userClient.getUserById(10L)).thenReturn(user1);
            when(userClient.getUserById(20L)).thenReturn(user2);

            Page<OrderResponseDto> result = orderService.getAllOrders(null, null, null, pageable);

            assertNotNull(result);
            assertEquals(2, result.getContent().size());

            verify(userClient, times(1)).getUserById(10L);
            verify(userClient, times(1)).getUserById(20L);
            verify(orderDao).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should throw OrderNullParametrException when pageable is null")
        void shouldThrowExceptionWhenPageableIsNull() {
            assertThrows(OrderNullParametrException.class,
                    () -> orderService.getAllOrders(null, null, null, null));

            verifyNoInteractions(orderDao, userClient);
        }

        @Test
        @DisplayName("Should return empty page when no orders found")
        void shouldReturnEmptyPageWhenNoOrders() {
            Pageable pageable = PageRequest.of(0, 10);
            when(orderDao.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            Page<OrderResponseDto> result = orderService.getAllOrders(null, null, null, pageable);

            assertTrue(result.isEmpty());
            verifyNoInteractions(orderMapper, userClient);
        }
    }

    @Nested
    @DisplayName("Set Paid Status Tests")
    class SetPaidStatusTests {

        @Test
        @DisplayName("Should successfully set PAID status and return DTO")
        void shouldSetPaidStatusSuccessfully() {
            Long orderId = 1L;
            Long userId = 10L;
            Order order = Order.builder()
                    .id(orderId)
                    .userId(userId)
                    .status(Status.NEW)
                    .build();

            OrderResponseDto responseDto = new OrderResponseDto();
            UserDto userDto = UserDto.builder().id(userId).name("Nikita").build();

            when(orderDao.findById(orderId)).thenReturn(Optional.of(order));
            when(orderMapper.toOrderDto(order)).thenReturn(responseDto);
            when(userClient.getUserById(userId)).thenReturn(userDto);

            OrderResponseDto result = orderService.setPaidStatus(orderId);

            assertNotNull(result);
            assertEquals(Status.PAID, order.getStatus());
            assertEquals(userDto, result.getUserDto());

            verify(orderDao).findById(orderId);
            verify(userClient).getUserById(userId);
        }

        @Test
        @DisplayName("Should throw OrderNullParametrException when id is null")
        void shouldThrowExceptionWhenIdIsNull() {
            assertThrows(OrderNullParametrException.class, () -> orderService.setPaidStatus(null));

            verifyNoInteractions(orderDao, userClient);
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order does not exist")
        void shouldThrowExceptionWhenOrderNotFound() {
            Long orderId = 1L;
            when(orderDao.findById(orderId)).thenReturn(Optional.empty());

            assertThrows(OrderNotFoundException.class, () -> orderService.setPaidStatus(orderId));

            verify(orderDao).findById(orderId);
            verifyNoInteractions(orderMapper, userClient);
        }

        @Test
        @DisplayName("Should throw OrderCancelledException when trying to pay for a cancelled order")
        void shouldThrowOrderCancelledExceptionWhenOrderIsCancelled() {
            Long orderId = 1L;
            Order cancelledOrder = Order.builder()
                    .id(orderId)
                    .status(Status.CANCELLED)
                    .build();

            when(orderDao.findById(orderId)).thenReturn(Optional.of(cancelledOrder));

            assertThrows(OrderCancelledException.class, () -> orderService.setPaidStatus(orderId));

            verify(orderDao).findById(orderId);
            verifyNoInteractions(orderMapper, userClient);
        }

        @Test
        @DisplayName("Should still return success even if order is already PAID (Idempotency)")
        void shouldReturnSuccessWhenOrderAlreadyPaid() {
            Long orderId = 1L;
            Long userId = 10L;
            Order alreadyPaidOrder = Order.builder()
                    .id(orderId)
                    .userId(userId)
                    .status(Status.PAID)
                    .build();

            OrderResponseDto responseDto = new OrderResponseDto();
            UserDto userDto = UserDto.builder().id(userId).name("Nikita").build();

            when(orderDao.findById(orderId)).thenReturn(Optional.of(alreadyPaidOrder));
            when(orderMapper.toOrderDto(alreadyPaidOrder)).thenReturn(responseDto);
            when(userClient.getUserById(userId)).thenReturn(userDto);

            OrderResponseDto result = orderService.setPaidStatus(orderId);

            assertNotNull(result);
            assertEquals(Status.PAID, alreadyPaidOrder.getStatus());
            verify(orderDao).findById(orderId);
        }
    }

    @Nested
    @DisplayName("Update Order Tests")
    class UpdateOrderTests {

        @Test
        @DisplayName("Should update order successfully")
        void shouldUpdateOrderSuccessfully() {
            Long orderId = 1L;
            Long userId = 10L;
            OrderItemRequestDto itemRequest = new OrderItemRequestDto(5L, 2);
            OrderRequestDto requestDto = new OrderRequestDto(userId, List.of(itemRequest));

            Order existingOrder = Order.builder()
                    .id(orderId)
                    .userId(userId)
                    .orderItems(new ArrayList<>(List.of(new OrderItem())))
                    .deleted(false)
                    .build();

            Item item = Item.builder().id(5L).price(100L).build();
            UserDto userDto = UserDto.builder().id(userId).name("Nikita").build();

            when(orderDao.findById(orderId)).thenReturn(Optional.of(existingOrder));
            when(itemDao.findById(5L)).thenReturn(Optional.of(item));
            when(orderDao.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
            when(orderMapper.toOrderDto(any())).thenReturn(new OrderResponseDto());
            when(userClient.getUserById(userId)).thenReturn(userDto);

            OrderResponseDto result = orderService.updateOrder(orderId, requestDto);

            assertNotNull(result);
            assertEquals(200L, existingOrder.getTotalPrice());
            assertTrue(existingOrder.getOrderItems().size() == 1);

            verify(orderMapper).updateOrder(eq(requestDto), eq(existingOrder));
            verify(orderDao).save(existingOrder);
        }

        @Test
        @DisplayName("Should throw OrderNullParametrException when request is null")
        void shouldThrowExceptionWhenRequestIsNull() {
            assertThrows(OrderNullParametrException.class,
                    () -> orderService.updateOrder(1L, null));
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order does not exist")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(orderDao.findById(1L)).thenReturn(Optional.empty());

            assertThrows(OrderNotFoundException.class,
                    () -> orderService.updateOrder(1L, new OrderRequestDto()));
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order is marked as deleted")
        void shouldThrowExceptionWhenOrderIsDeleted() {
            Order deletedOrder = Order.builder().id(1L).deleted(true).build();
            when(orderDao.findById(1L)).thenReturn(Optional.of(deletedOrder));

            assertThrows(OrderNotFoundException.class,
                    () -> orderService.updateOrder(1L, new OrderRequestDto()));

            verify(orderMapper, never()).updateOrder(any(), any());
        }

        @Test
        @DisplayName("Should throw ItemNotFoundException when item in updated list not found")
        void shouldThrowExceptionWhenItemInUpdateNotFound() {
            Long orderId = 1L;
            OrderItemRequestDto itemRequest = new OrderItemRequestDto(99L, 1);
            OrderRequestDto requestDto = new OrderRequestDto(10L, List.of(itemRequest));
            Order existingOrder = Order.builder().id(orderId).orderItems(new ArrayList<>()).deleted(false).build();

            when(orderDao.findById(orderId)).thenReturn(Optional.of(existingOrder));
            when(itemDao.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ItemNotFoundException.class,
                    () -> orderService.updateOrder(orderId, requestDto));
        }
    }

    @Nested
    @DisplayName("Soft Delete Order Tests")
    class SoftDeleteOrderTests {

        @Test
        @DisplayName("Should successfully mark order as deleted")
        void shouldSoftDeleteOrderSuccessfully() {
            Long orderId = 1L;
            Order order = Order.builder()
                    .id(orderId)
                    .deleted(false)
                    .build();

            when(orderDao.findById(orderId)).thenReturn(Optional.of(order));

            boolean result = orderService.softdeleteOrder(orderId);

            assertTrue(result);
            assertTrue(order.isDeleted());

            verify(orderDao, times(1)).findById(orderId);
        }

        @Test
        @DisplayName("Should throw OrderNullParametrException when id is null")
        void shouldThrowExceptionWhenIdIsNull() {
            assertThrows(OrderNullParametrException.class, () -> orderService.softdeleteOrder(null));

            verifyNoInteractions(orderDao);
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order does not exist")
        void shouldThrowExceptionWhenOrderNotFound() {
            Long orderId = 1L;
            when(orderDao.findById(orderId)).thenReturn(Optional.empty());

            assertThrows(OrderNotFoundException.class, () -> orderService.softdeleteOrder(orderId));

            verify(orderDao, times(1)).findById(orderId);
        }
        @Test
        @DisplayName("Should throw OrderNotFoundException when order is already deleted")
        void shouldThrowExceptionWhenOrderAlreadyDeleted() {
            Long orderId = 1L;
            Order alreadyDeletedOrder = Order.builder()
                    .id(orderId)
                    .deleted(true)
                    .build();

            when(orderDao.findById(orderId)).thenReturn(Optional.of(alreadyDeletedOrder));

            assertThrows(OrderNotFoundException.class, () -> orderService.softdeleteOrder(orderId));

            verify(orderDao, times(1)).findById(orderId);
        }
    }

}