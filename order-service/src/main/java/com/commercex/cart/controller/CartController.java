package com.commercex.cart.controller;

import com.commercex.cart.dto.AddToCartRequest;
import com.commercex.cart.dto.CartResponseDTO;
import com.commercex.cart.dto.UpdateCartItemRequest;
import com.commercex.cart.service.CartService;
import com.commercex.order.client.UserServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Shopping Cart", description = "Cart item management")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserServiceClient userServiceClient;

    @Operation(summary = "Get current user cart")
    @GetMapping
    public ResponseEntity<CartResponseDTO> getCart(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @Operation(summary = "Add item to cart")
    @PostMapping("/items")
    public ResponseEntity<CartResponseDTO> addItem(
            Authentication authentication,
            @Valid @RequestBody AddToCartRequest request) {
        Long userId = resolveUserId(authentication);
        return new ResponseEntity<>(cartService.addItem(userId, request), HttpStatus.CREATED);
    }

    @Operation(summary = "Update item quantity")
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDTO> updateItemQuantity(
            Authentication authentication,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, itemId, request));
    }

    @Operation(summary = "Remove item from cart")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            Authentication authentication,
            @PathVariable Long itemId) {
        Long userId = resolveUserId(authentication);
        cartService.removeItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Clear all items from cart")
    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userServiceClient.getUserByEmail(email).getId();
    }
}
