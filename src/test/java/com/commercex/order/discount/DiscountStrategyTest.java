package com.commercex.order.discount;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class DiscountStrategyTest {

    @Test
    @DisplayName("NoDiscount returns zero")
    void noDiscount() {
        DiscountStrategy strategy = new NoDiscount();
        assertThat(strategy.calculateDiscount(new BigDecimal("1000.00")))
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("PercentageDiscount calculates correctly (10% of 999.99)")
    void percentageDiscount() {
        DiscountStrategy strategy = new PercentageDiscount(new BigDecimal("0.10"));
        assertThat(strategy.calculateDiscount(new BigDecimal("999.99")))
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("PercentageDiscount 20% of 250.00")
    void percentageDiscount_20() {
        DiscountStrategy strategy = new PercentageDiscount(new BigDecimal("0.20"));
        assertThat(strategy.calculateDiscount(new BigDecimal("250.00")))
                .isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("FlatDiscount returns flat amount when less than total")
    void flatDiscount_underTotal() {
        DiscountStrategy strategy = new FlatDiscount(new BigDecimal("50.00"));
        assertThat(strategy.calculateDiscount(new BigDecimal("200.00")))
                .isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("FlatDiscount is capped at order total")
    void flatDiscount_cappedAtTotal() {
        DiscountStrategy strategy = new FlatDiscount(new BigDecimal("100.00"));
        assertThat(strategy.calculateDiscount(new BigDecimal("30.00")))
                .isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("DiscountStrategyFactory returns correct strategies")
    void factory_strategies() {
        DiscountStrategyFactory factory = new DiscountStrategyFactory();

        assertThat(factory.getStrategy(null)).isInstanceOf(NoDiscount.class);
        assertThat(factory.getStrategy("")).isInstanceOf(NoDiscount.class);
        assertThat(factory.getStrategy("SAVE10")).isInstanceOf(PercentageDiscount.class);
        assertThat(factory.getStrategy("FLAT50")).isInstanceOf(FlatDiscount.class);
    }

    @Test
    @DisplayName("DiscountStrategyFactory validates promo codes")
    void factory_validation() {
        DiscountStrategyFactory factory = new DiscountStrategyFactory();

        assertThat(factory.isValidPromoCode("SAVE10")).isTrue();
        assertThat(factory.isValidPromoCode("SAVE20")).isTrue();
        assertThat(factory.isValidPromoCode("FLAT50")).isTrue();
        assertThat(factory.isValidPromoCode("WELCOME")).isTrue();
        assertThat(factory.isValidPromoCode("BOGUS")).isFalse();
        assertThat(factory.isValidPromoCode(null)).isTrue(); // no promo = valid
        assertThat(factory.isValidPromoCode("")).isTrue();
    }
}
