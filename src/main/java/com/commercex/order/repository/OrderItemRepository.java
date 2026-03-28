package com.commercex.order.repository;

import com.commercex.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for OrderItem entity.
 * Most operations go through Order's cascade, but available for direct queries.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
