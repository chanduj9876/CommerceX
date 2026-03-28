package com.commercex.cart.service;

import com.commercex.cart.dto.AddToCartRequest;
import com.commercex.cart.dto.CartResponseDTO;
import com.commercex.cart.dto.UpdateCartItemRequest;
import com.commercex.cart.entity.Cart;
import com.commercex.cart.entity.CartItem;
import com.commercex.cart.mapper.CartMapper;
import com.commercex.cart.repository.CartRepository;
import com.commercex.common.InsufficientStockException;
import com.commercex.common.ResourceNotFoundException;
import com.commercex.product.entity.Product;
import com.commercex.product.repository.ProductRepository;
import com.commercex.user.entity.User;
import com.commercex.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cart service implementation.
 *
 * Business rules:
 * - One cart per user, lazily created on first addItem
 * - Adding a product that already exists in the cart increases its quantity
 * - Quantity must be at least 1 (validated by DTO)
 * - Stock is NOT reserved at cart time — only checked/deducted at checkout (OrderService)
 * - Product must exist and have stock > 0 to be added
 *
 * Why lazy cart creation? Avoids creating empty carts for users who never shop.
 * The cart is created in addItem and returned as empty from getCart if it doesn't exist.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "carts", key = "#userId")
    public CartResponseDTO getCart(Long userId) {
        log.info("[CACHE MISS] Cart for user {} not in cache — loading from DB", userId);
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElse(null);

        if (cart == null) {
            // Return empty cart response — user hasn't added anything yet
            return CartResponseDTO.builder()
                    .items(java.util.List.of())
                    .totalItems(0)
                    .totalPrice(java.math.BigDecimal.ZERO)
                    .build();
        }

        return CartMapper.toResponseDTO(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = "carts", key = "#userId")
    public CartResponseDTO addItem(Long userId, AddToCartRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        if (product.getStock() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock for product: " + product.getName() +
                    ". Available: " + product.getStock() + ", requested: " + request.getQuantity());
        }

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createCartForUser(userId));

        CartItem newItem = CartItem.builder()
                .product(product)
                .quantity(request.getQuantity())
                .build();

        cart.addItem(newItem);
        cartRepository.save(cart);

        // Re-fetch with JOIN FETCH to ensure products are loaded for the response
        Cart refreshed = cartRepository.findByUserIdWithItems(userId).orElseThrow();
        return CartMapper.toResponseDTO(refreshed);
    }

    @Override
    @Transactional
    @CacheEvict(value = "carts", key = "#userId")
    public CartResponseDTO updateItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + cartItemId));

        if (item.getProduct().getStock() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock for product: " +
                    item.getProduct().getName() + ". Available: " + item.getProduct().getStock() +
                    ", requested: " + request.getQuantity());
        }

        item.setQuantity(request.getQuantity());
        cartRepository.save(cart);

        return CartMapper.toResponseDTO(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = "carts", key = "#userId")
    public void removeItem(Long userId, Long cartItemId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(cartItemId));
        if (!removed) {
            throw new ResourceNotFoundException("Cart item not found with id: " + cartItemId);
        }

        cartRepository.save(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = "carts", key = "#userId")
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cart.clear();
        cartRepository.save(cart);
    }

    private Cart createCartForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return Cart.builder()
                .user(user)
                .build();
    }
}
