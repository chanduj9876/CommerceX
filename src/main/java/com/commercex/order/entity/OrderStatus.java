package com.commercex.order.entity;

/**
 * Order lifecycle status.
 *
 * State transitions:
 *   PENDING → CONFIRMED → SHIPPED → DELIVERED
 *   PENDING → CANCELLED
 *   CONFIRMED → CANCELLED (before shipping)
 *
 * PENDING:    Order created, awaiting payment confirmation
 * CONFIRMED:  Payment received, ready for fulfillment
 * SHIPPED:    Handed off to carrier
 * DELIVERED:  Customer received the package
 * CANCELLED:  Order cancelled (by customer or admin)
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
