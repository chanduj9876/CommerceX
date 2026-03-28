package com.commercex.cart.controller;

import com.commercex.cart.dto.AddToCartRequest;
import com.commercex.cart.dto.CartResponseDTO;
import com.commercex.cart.dto.UpdateCartItemRequest;
import com.commercex.cart.service.CartService;
import com.commercex.user.entity.User;
import com.commercex.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Shopping cart controller.
 *
 * All endpoints require authentication (JWT token).
 * The user is identified from the JWT — no userId in the URL.
 *
 * Endpoints:
 *   GET    /api/v1/cart              — view current cart
 *   POST   /api/v1/cart/items        — add product to cart
 *   PUT    /api/v1/cart/items/{id}   — update item quantity
 *   DELETE /api/v1/cart/items/{id}   — remove item from cart
 *   DELETE /api/v1/cart              — clear entire cart
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<CartResponseDTO> getCart(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDTO> addItem(
            Authentication authentication,
            @Valid @RequestBody AddToCartRequest request) {
        Long userId = resolveUserId(authentication);
        return new ResponseEntity<>(cartService.addItem(userId, request), HttpStatus.CREATED);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDTO> updateItemQuantity(
            Authentication authentication,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            Authentication authentication,
            @PathVariable Long itemId) {
        Long userId = resolveUserId(authentication);
        cartService.removeItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Resolves the user ID from the JWT token.
     * The JWT subject is the email — we look up the user to get the ID.
     */
    private Long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.commercex.common.ResourceNotFoundException("User not found"))
                .getId();
    }
}
