package com.commercex.order.discount;

import java.math.BigDecimal;

public interface DiscountStrategy {

    BigDecimal calculateDiscount(BigDecimal total);
}
