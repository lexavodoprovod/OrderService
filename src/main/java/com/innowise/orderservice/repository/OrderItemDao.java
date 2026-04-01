package com.innowise.orderservice.repository;

import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderItemDao extends JpaRepository<OrderItem,Long>, JpaSpecificationExecutor<Order> {
}
