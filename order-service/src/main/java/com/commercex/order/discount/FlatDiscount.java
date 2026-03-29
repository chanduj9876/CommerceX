package com.commercex.order.discount;

import java.math.BigDecimal;

public class FlatDiscount implements DiscountStrategy {

    private final BigDecimal amount;

    public FlatDiscount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal total) {
        return amount.min(total);
    }
}
