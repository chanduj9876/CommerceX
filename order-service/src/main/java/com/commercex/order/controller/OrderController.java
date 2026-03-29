package com.commercex.order.controller;

import com.commercex.order.client.UserServiceClient;
import com.commercex.order.dto.CreateOrderRequest;
import com.commercex.order.dto.OrderResponseDTO;
import com.commercex.order.dto.UpdateOrderStatusRequest;
import com.commercex.order.entity.OrderStatus;
import com.commercex.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Orders", description = "Order placement and management")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserServiceClient userServiceClient;

    @Operation(summary = "Create an order from current cart")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponseDTO> createOrder(
            Authentication authentication,
            @Valid @RequestBody(required = false) CreateOrderRequest request) {
        Long userId = resolveUserId(authentication);
        OrderResponseDTO order = orderService.createOrder(userId, request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @Operation(summary = "Get current user orders")
    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderResponseDTO>> getMyOrders(
            Authentication authentication,
            @PageableDefault(size = 10) Pageable pageable) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(orderService.getMyOrders(userId, pageable));
    }

    @Operation(summary = "Get order by ID")
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

    @Operation(summary = "Cancel an order")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponseDTO> cancelOrder(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(orderService.cancelOrder(userId, id));
    }

    @Operation(summary = "List all orders (admin)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponseDTO>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(status, pageable));
    }

    @Operation(summary = "Update order status (admin)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.getStatus()));
    }

    private Long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userServiceClient.getUserByEmail(email).getId();
    }
}
