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
import com.commercex.order.client.ProductDTO;
import com.commercex.order.client.ProductServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductServiceClient productServiceClient;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "carts", key = "#userId")
    public CartResponseDTO getCart(Long userId) {
        log.info("[CACHE MISS] Cart for user {} not in cache — loading from DB", userId);
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElse(null);

        if (cart == null) {
            return CartResponseDTO.builder()
                    .items(List.of())
                    .totalItems(0)
                    .totalPrice(BigDecimal.ZERO)
                    .build();
        }

        Map<Long, ProductDTO> productMap = fetchProductMap(cart);
        return CartMapper.toResponseDTO(cart, productMap);
    }

    @Override
    @Transactional
    @CacheEvict(value = "carts", key = "#userId")
    public CartResponseDTO addItem(Long userId, AddToCartRequest request) {
        ProductDTO product = productServiceClient.getProduct(request.getProductId());

        if (product.getStock() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock for product: " + product.getName() +
                    ". Available: " + product.getStock() + ", requested: " + request.getQuantity());
        }

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createCartForUser(userId));

        CartItem newItem = CartItem.builder()
                .productId(product.getId())
                .quantity(request.getQuantity())
                .build();

        cart.addItem(newItem);
        cartRepository.save(cart);

        Cart refreshed = cartRepository.findByUserIdWithItems(userId).orElseThrow();
        Map<Long, ProductDTO> productMap = fetchProductMap(refreshed);
        return CartMapper.toResponseDTO(refreshed, productMap);
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

        ProductDTO product = productServiceClient.getProduct(item.getProductId());
        if (product.getStock() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock for product: " +
                    product.getName() + ". Available: " + product.getStock() +
                    ", requested: " + request.getQuantity());
        }

        item.setQuantity(request.getQuantity());
        cartRepository.save(cart);

        Map<Long, ProductDTO> productMap = fetchProductMap(cart);
        return CartMapper.toResponseDTO(cart, productMap);
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
        return Cart.builder()
                .userId(userId)
                .build();
    }

    private Map<Long, ProductDTO> fetchProductMap(Cart cart) {
        Set<Long> productIds = cart.getItems().stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toSet());

        return productIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            try {
                                return productServiceClient.getProduct(id);
                            } catch (Exception e) {
                                log.warn("Failed to fetch product {}: {}", id, e.getMessage());
                                return ProductDTO.builder()
                                        .id(id)
                                        .name("Unknown Product")
                                        .price(BigDecimal.ZERO)
                                        .stock(0)
                                        .build();
                            }
                        }
                ));
    }
}
