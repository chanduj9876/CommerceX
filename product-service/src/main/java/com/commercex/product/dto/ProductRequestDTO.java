package com.commercex.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;

@Schema(description = "Create or replace a product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDTO {

    @Schema(description = "Product name", example = "Wireless Headphones")
    @NotBlank(message = "Product name is required")
    private String name;

    @Schema(description = "Product description", example = "Noise-cancelling over-ear headphones")
    private String description;

    @Schema(description = "Price in USD", example = "99.99")
    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    @Schema(description = "Available stock quantity", example = "50")
    @PositiveOrZero(message = "Stock cannot be negative")
    private Integer stock;

    @Schema(description = "Set of category IDs to assign", example = "[1, 3]")
    private Set<Long> categoryIds;
}
