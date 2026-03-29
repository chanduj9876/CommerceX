package com.commercex.order.service;

import com.commercex.order.dto.CreateOrderRequest;
import com.commercex.order.dto.OrderResponseDTO;
import com.commercex.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponseDTO createOrder(Long userId, CreateOrderRequest request);

    OrderResponseDTO getOrderById(Long orderId, Long userId);

    OrderResponseDTO getOrderById(Long orderId);

    Page<OrderResponseDTO> getMyOrders(Long userId, Pageable pageable);

    Page<OrderResponseDTO> getAllOrders(OrderStatus status, Pageable pageable);

    OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus newStatus);

    OrderResponseDTO cancelOrder(Long userId, Long orderId);
}
