package com.commercex.order.discount;

import java.math.BigDecimal;

public class NoDiscount implements DiscountStrategy {

    @Override
    public BigDecimal calculateDiscount(BigDecimal total) {
        return BigDecimal.ZERO;
    }
}
