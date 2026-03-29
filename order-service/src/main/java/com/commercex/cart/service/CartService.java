package com.commercex.cart.service;

import com.commercex.cart.dto.AddToCartRequest;
import com.commercex.cart.dto.CartResponseDTO;
import com.commercex.cart.dto.UpdateCartItemRequest;

public interface CartService {

    CartResponseDTO getCart(Long userId);

    CartResponseDTO addItem(Long userId, AddToCartRequest request);

    CartResponseDTO updateItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequest request);

    void removeItem(Long userId, Long cartItemId);

    void clearCart(Long userId);
}
