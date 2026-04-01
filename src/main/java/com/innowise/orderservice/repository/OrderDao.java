package com.innowise.orderservice.repository;

import com.innowise.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;

public interface OrderDao extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    String SET_DELETED_TRUE_JPQL = """
            update Order o
            set deleted=true
            where o.id = :id
            """;

    @Modifying
    @Query(value = SET_DELETED_TRUE_JPQL)
    int softDeleteById(Long id);

    @Override
    @EntityGraph(attributePaths = {"orderItems"})
    Page<Order> findAll(Specification<Order> spec,  Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems"})
    Page<Order> findAllByUserIdAndDeletedFalse(Long userID, Pageable pageable);
}
