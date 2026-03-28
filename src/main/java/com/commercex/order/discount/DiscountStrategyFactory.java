package com.commercex.order.discount;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry-based Factory for selecting the correct discount strategy based on a promo code.
 *
 * Why registry instead of switch? Open/Closed Principle (OCP).
 * Adding a new promo code only requires:
 *   registerPromoCode("SUMMER30", () -> new PercentageDiscount(new BigDecimal("0.30")));
 * No modification to this class needed.
 *
 * Current promo codes (simulated):
 *   SAVE10   → 10% off
 *   SAVE20   → 20% off
 *   FLAT50   → $50 off
 *   WELCOME  → 15% off (new customer welcome offer)
 *   null/blank → no discount
 *
 * In a real system, these would come from a database promo_codes table.
 */
@Component
public class DiscountStrategyFactory {

    private static final Map<String, Supplier<DiscountStrategy>> registry = new HashMap<>();

    static {
        registry.put("SAVE10", () -> new PercentageDiscount(new BigDecimal("0.10")));
        registry.put("SAVE20", () -> new PercentageDiscount(new BigDecimal("0.20")));
        registry.put("FLAT50", () -> new FlatDiscount(new BigDecimal("50.00")));
        registry.put("WELCOME", () -> new PercentageDiscount(new BigDecimal("0.15")));
    }

    /**
     * Register a new promo code at runtime (OCP — extend without modifying).
     */
    public static void registerPromoCode(String code, Supplier<DiscountStrategy> creator) {
        registry.put(code.toUpperCase(), creator);
    }

    /**
     * Returns the appropriate discount strategy for the given promo code.
     */
    public DiscountStrategy getStrategy(String promoCode) {
        if (promoCode == null || promoCode.isBlank()) {
            return new NoDiscount();
        }

        Supplier<DiscountStrategy> creator = registry.get(promoCode.toUpperCase());
        return (creator != null) ? creator.get() : new NoDiscount();
    }

    /**
     * Checks if a promo code is valid (registered in the factory).
     */
    public boolean isValidPromoCode(String promoCode) {
        if (promoCode == null || promoCode.isBlank()) {
            return true; // No promo code is valid (just no discount)
        }
        return registry.containsKey(promoCode.toUpperCase());
    }
}
