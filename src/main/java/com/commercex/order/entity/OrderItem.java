package com.commercex.order.entity;

import com.commercex.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A single line item in an order — snapshots the product data at order time.
 *
 * Why snapshot instead of referencing Product?
 * - Product prices can change after the order is placed
 * - Products can be deleted — the order history must survive
 * - The customer paid THIS price for THIS quantity — that's immutable
 *
 * We store productId for reference/lookup, but the order item is self-contained.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    /**
     * Returns the line total (unitPrice × quantity).
     */
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
