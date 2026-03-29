package com.commercex.order.controller;

import com.commercex.common.ResourceNotFoundException;
import com.commercex.order.entity.Order;
import com.commercex.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderRepository orderRepository;

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + id));
        return ResponseEntity.ok(Map.of(
                "id", order.getId(),
                "userId", order.getUserId(),
                "userEmail", order.getUserEmail(),
                "totalAmount", order.getTotalAmount(),
                "status", order.getStatus().name()
        ));
    }
}
