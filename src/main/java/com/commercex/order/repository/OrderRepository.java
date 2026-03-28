package com.commercex.order.repository;

import com.commercex.order.entity.Order;
import com.commercex.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repository for Order entity.
 *
 * Why JOIN FETCH? Loading an order almost always requires its items.
 * Eager-fetching avoids N+1 queries in the service layer.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items"})
    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(Long id);

    @EntityGraph(attributePaths = {"items"})
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
