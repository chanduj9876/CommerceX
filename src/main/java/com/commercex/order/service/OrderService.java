package com.commercex.order.service;

import com.commercex.order.dto.CreateOrderRequest;
import com.commercex.order.dto.OrderResponseDTO;
import com.commercex.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Order service interface.
 *
 * createOrder: Converts cart → order (snapshots products, applies discount, deducts stock, clears cart)
 * getOrderById: Returns a single order with items
 * getMyOrders: Returns paginated orders for the authenticated user
 * getAllOrders: Admin-only, returns all orders with optional status filter
 * updateOrderStatus: Admin-only, transitions order status
 * cancelOrder: Customer can cancel their own PENDING order
 */
public interface OrderService {

    OrderResponseDTO createOrder(Long userId, CreateOrderRequest request);

    OrderResponseDTO getOrderById(Long orderId, Long userId);

    /** Admin overload — no ownership check */
    OrderResponseDTO getOrderById(Long orderId);

    Page<OrderResponseDTO> getMyOrders(Long userId, Pageable pageable);

    Page<OrderResponseDTO> getAllOrders(OrderStatus status, Pageable pageable);

    OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus newStatus);

    OrderResponseDTO cancelOrder(Long userId, Long orderId);
}
