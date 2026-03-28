package com.commercex.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Request DTO — what the client sends when creating/updating a product.
 *
 * Why a separate DTO from the entity?
 * - Client shouldn't send id or timestamps (server controls those)
 * - Validation annotations live here, keeping the entity clean
 * - Decouples API contract from database schema — you can change one without breaking the other
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDTO {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    @PositiveOrZero(message = "Stock cannot be negative")
    private Integer stock;

    private Set<Long> categoryIds;
}
