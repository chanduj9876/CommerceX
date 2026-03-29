package com.commercex.order.discount;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class DiscountStrategyFactory {

    private static final Map<String, Supplier<DiscountStrategy>> registry = new HashMap<>();

    static {
        registry.put("SAVE10", () -> new PercentageDiscount(new BigDecimal("0.10")));
        registry.put("SAVE20", () -> new PercentageDiscount(new BigDecimal("0.20")));
        registry.put("FLAT50", () -> new FlatDiscount(new BigDecimal("50.00")));
        registry.put("WELCOME", () -> new PercentageDiscount(new BigDecimal("0.15")));
    }

    public static void registerPromoCode(String code, Supplier<DiscountStrategy> creator) {
        registry.put(code.toUpperCase(), creator);
    }

    public DiscountStrategy getStrategy(String promoCode) {
        if (promoCode == null || promoCode.isBlank()) {
            return new NoDiscount();
        }

        Supplier<DiscountStrategy> creator = registry.get(promoCode.toUpperCase());
        return (creator != null) ? creator.get() : new NoDiscount();
    }

    public boolean isValidPromoCode(String promoCode) {
        if (promoCode == null || promoCode.isBlank()) {
            return true;
        }
        return registry.containsKey(promoCode.toUpperCase());
    }
}
