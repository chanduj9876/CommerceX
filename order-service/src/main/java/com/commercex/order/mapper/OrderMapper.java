package com.commercex.order.mapper;

import com.commercex.order.dto.OrderItemResponseDTO;
import com.commercex.order.dto.OrderResponseDTO;
import com.commercex.order.entity.Order;
import com.commercex.order.entity.OrderItem;

import java.util.List;

public class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponseDTO toResponseDTO(Order order) {
        List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                .map(OrderMapper::toItemResponseDTO)
                .toList();

        var subtotal = order.getTotalAmount().add(order.getDiscountAmount());

        return OrderResponseDTO.builder()
                .id(order.getId())
                .userEmail(order.getUserEmail())
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
