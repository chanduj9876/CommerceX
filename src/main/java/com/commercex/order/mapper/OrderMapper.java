package com.commercex.order.mapper;

import com.commercex.order.dto.OrderItemResponseDTO;
import com.commercex.order.dto.OrderResponseDTO;
import com.commercex.order.entity.Order;
import com.commercex.order.entity.OrderItem;

import java.util.List;

/**
 * Maps Order/OrderItem entities to response DTOs.
 */
public class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponseDTO toResponseDTO(Order order) {
        List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                .map(OrderMapper::toItemResponseDTO)
                .toList();

        // Subtotal = totalAmount + discountAmount (what they would have paid without discount)
        var subtotal = order.getTotalAmount().add(order.getDiscountAmount());

        return OrderResponseDTO.builder()
                .id(order.getId())
                .userEmail(order.getUser().getEmail())
                .items(itemDTOs)
                .subtotal(subtotal)
                .discountAmount(order.getDiscountAmount())
                .promoCode(order.getPromoCode())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public static OrderItemResponseDTO toItemResponseDTO(OrderItem item) {
        return OrderItemResponseDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
