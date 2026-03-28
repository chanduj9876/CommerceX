package com.commercex.cart.entity;

import com.commercex.common.BaseEntity;
import com.commercex.product.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * A single line item in a shopping cart.
 *
 * Why @ManyToOne to Product? A cart item references a live product — if the
 * product's price changes before checkout, the customer sees the current price.
 * (At checkout, we snapshot the price into OrderItem so it's locked in.)
 *
 * Why @ManyToOne to Cart? Each item belongs to exactly one cart.
 * FetchType.LAZY avoids loading the full cart when we just need the item.
 */
@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Positive(message = "Quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;
}
