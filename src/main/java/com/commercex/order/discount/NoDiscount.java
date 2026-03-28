package com.commercex.order.discount;

import java.math.BigDecimal;

/**
 * No discount — returns zero. Used when no promo code is provided.
 */
public class NoDiscount implements DiscountStrategy {

    @Override
    public BigDecimal calculateDiscount(BigDecimal total) {
        return BigDecimal.ZERO;
    }
}
