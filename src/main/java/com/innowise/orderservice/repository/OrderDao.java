package com.innowise.orderservice.repository;

import com.innowise.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderDao extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    Optional<Order> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    Page<Order> findAll(Specification<Order> spec,  Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    Page<Order> findAllByUserIdAndDeletedFalse(Long userID, Pageable pageable);
}
