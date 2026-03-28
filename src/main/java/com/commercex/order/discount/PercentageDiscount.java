package com.commercex.order.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Percentage-based discount (e.g., 10% off).
 *
 * Rate is a decimal: 0.10 = 10%, 0.25 = 25%.
 * Result is rounded to 2 decimal places (standard currency precision).
 */
public class PercentageDiscount implements DiscountStrategy {

    private final BigDecimal rate;

    public PercentageDiscount(BigDecimal rate) {
        this.rate = rate;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal total) {
        return total.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
