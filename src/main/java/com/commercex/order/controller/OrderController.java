package com.commercex.order.controller;

import com.commercex.order.dto.CreateOrderRequest;
import com.commercex.order.dto.OrderResponseDTO;
import com.commercex.order.dto.UpdateOrderStatusRequest;
import com.commercex.order.entity.OrderStatus;
import com.commercex.order.service.OrderService;
import com.commercex.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Order controller.
 *
 * Customer endpoints:
 *   POST   /api/v1/orders              — place order (checkout from cart)
 *   GET    /api/v1/orders/my-orders    — list my orders (paginated)
 *   GET    /api/v1/orders/{id}         — view single order
 *   PATCH  /api/v1/orders/{id}/cancel  — cancel my PENDING order
 *
 * Admin endpoints:
 *   GET    /api/v1/orders              — list all orders (with optional status filter)
 *   PATCH  /api/v1/orders/{id}/status  — update order status
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponseDTO> createOrder(
            Authentication authentication,
            @Valid @RequestBody(required = false) CreateOrderRequest request) {
        Long userId = resolveUserId(authentication);
        OrderResponseDTO order = orderService.createOrder(userId, request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderResponseDTO>> getMyOrders(
            Authentication authentication,
            @PageableDefault(size = 10) Pageable pageable) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(orderService.getMyOrders(userId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponseDTO> getOrderById(
            Authentication authentication,
            @PathVariable Long id) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return ResponseEntity.ok(orderService.getOrderById(id));
        }
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(orderService.getOrderById(id, userId));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponseDTO> cancelOrder(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(orderService.cancelOrder(userId, id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponseDTO>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(status, pageable));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.getStatus()));
    }

    private Long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.commercex.common.ResourceNotFoundException("User not found"))
                .getId();
    }
}
