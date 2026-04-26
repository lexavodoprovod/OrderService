package com.innowise.orderservice.repository;

import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ItemDao extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Order> {

    @Override
    Page<Item> findAll(Specification spec, Pageable pageable);

    boolean existsByName(String name);

    Optional<Item> findById(Long id);
}
