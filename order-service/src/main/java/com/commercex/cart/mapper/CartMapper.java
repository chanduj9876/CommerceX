package com.commercex.cart.mapper;

import com.commercex.cart.dto.CartItemResponseDTO;
import com.commercex.cart.dto.CartResponseDTO;
import com.commercex.cart.entity.Cart;
import com.commercex.cart.entity.CartItem;
import com.commercex.order.client.ProductDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CartMapper {

    private CartMapper() {
    }

    public static CartResponseDTO toResponseDTO(Cart cart, Map<Long, ProductDTO> productMap) {
        List<CartItemResponseDTO> itemDTOs = cart.getItems().stream()
                .map(item -> toItemResponseDTO(item, productMap.get(item.getProductId())))
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

    public static CartItemResponseDTO toItemResponseDTO(CartItem item, ProductDTO product) {
        String productName = product != null ? product.getName() : "Unknown Product";
        BigDecimal unitPrice = product != null ? product.getPrice() : BigDecimal.ZERO;
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponseDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(productName)
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}
