package com.commercex.order.discount;

import java.math.BigDecimal;

/**
 * Strategy pattern for applying discounts to order totals.
 *
 * Why Strategy pattern? Different discount types (percentage, flat, none)
 * have different calculation logic. The Strategy pattern lets us swap
 * discount algorithms without modifying the order service.
 *
 * Adding a new discount type (e.g., BuyOneGetOneFree) only requires:
 * 1. Implement this interface
 * 2. Register it in DiscountStrategyFactory
 * No changes to OrderService needed.
 */
public interface DiscountStrategy {

    /**
     * Applies the discount to the given total.
     *
     * @param total the original order subtotal
     * @return the discount amount (how much to subtract)
     */
    BigDecimal calculateDiscount(BigDecimal total);
}
