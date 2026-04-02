package com.innowise.orderservice.specification;

import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.Status;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    private OrderSpecification() {}

    public static Specification<Order> byDateRange(LocalDate from, LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }

            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to.atTime(LocalTime.MAX)));
            }

            if(predicates.isEmpty()){
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Order> byStatus(List<Status> statuses) {
        return (root, query, criteriaBuilder) ->{
            if(statuses.isEmpty() || statuses == null){
                return criteriaBuilder.conjunction();
            }

            return root.get("status").in(statuses);
        };
    }




        public static Specification<Order> notDeleted() {
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("deleted"), false));
    }
}
