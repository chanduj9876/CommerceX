package com.commercex.order.discount;

import java.math.BigDecimal;

/**
 * Flat discount — subtracts a fixed amount (e.g., $50 off).
 *
 * If the discount exceeds the total, the discount is capped at the total
 * (order total can't go negative).
 */
public class FlatDiscount implements DiscountStrategy {

    private final BigDecimal amount;

    public FlatDiscount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal total) {
        // Don't let the discount exceed the total
        return amount.min(total);
    }
}
