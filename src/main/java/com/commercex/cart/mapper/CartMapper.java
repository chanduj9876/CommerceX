package com.commercex.cart.mapper;

import com.commercex.cart.dto.CartItemResponseDTO;
import com.commercex.cart.dto.CartResponseDTO;
import com.commercex.cart.entity.Cart;
import com.commercex.cart.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;

/**
 * Maps Cart/CartItem entities to response DTOs.
 *
 * Why compute subtotal and totalPrice here? The client needs to display totals,
 * and computing them on the server ensures consistency (no floating-point drift
 * from client-side JS calculations).
 */
public class CartMapper {

    private CartMapper() {
    }

    public static CartResponseDTO toResponseDTO(Cart cart) {
        List<CartItemResponseDTO> itemDTOs = cart.getItems().stream()
                .map(CartMapper::toItemResponseDTO)
                .toList();

        BigDecimal totalPrice = itemDTOs.stream()
                .map(CartItemResponseDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return CartResponseDTO.builder()
                .id(cart.getId())
                .items(itemDTOs)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .build();
    }

    public static CartItemResponseDTO toItemResponseDTO(CartItem item) {
        BigDecimal subtotal = item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponseDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .unitPrice(item.getProduct().getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}
