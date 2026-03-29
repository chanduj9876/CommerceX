package com.commercex.order.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
