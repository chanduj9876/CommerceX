package com.commercex.product.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;

/**
 * DTO for partial updates (PATCH) — all fields are optional.
 *
 * null means "don't change this field." Only non-null fields get applied.
 * Validation annotations still apply when a value IS provided — you can skip
 * sending price, but if you do send it, it must be positive.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateDTO {

    private String name;

    private String description;

    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    @PositiveOrZero(message = "Stock cannot be negative")
    private Integer stock;

    private Set<Long> categoryIds;
}
