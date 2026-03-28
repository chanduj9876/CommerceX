package com.commercex.cart.service;

import com.commercex.cart.dto.AddToCartRequest;
import com.commercex.cart.dto.CartResponseDTO;
import com.commercex.cart.dto.UpdateCartItemRequest;

/**
 * Shopping cart service interface.
 *
 * All operations are scoped to the authenticated user (userId from JWT).
 * The cart is lazily created on first addItem call.
 */
public interface CartService {

    CartResponseDTO getCart(Long userId);

    CartResponseDTO addItem(Long userId, AddToCartRequest request);

    CartResponseDTO updateItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequest request);

    void removeItem(Long userId, Long cartItemId);

    void clearCart(Long userId);
}
