package com.commercex.order.entity;

import com.commercex.common.BaseEntity;
import com.commercex.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Order entity — represents a completed checkout.
 *
 * Created from a Cart: each CartItem becomes an OrderItem with snapshotted
 * product data (name, price at time of purchase).
 *
 * Why @ManyToOne to User (not @OneToOne)? A user can have many orders.
 *
 * Why separate totalAmount + discountAmount?
 * - totalAmount is the final amount the customer pays
 * - discountAmount records how much discount was applied (for reporting/auditing)
 * - Original subtotal = totalAmount + discountAmount
 *
 * Why promoCode field? Tracks which promo code was used (if any) for auditing.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String promoCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * Adds an item to this order and sets the back-reference.
     */
    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }
}
