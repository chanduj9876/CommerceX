package com.commercex.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;

@Schema(description = "Partial product update (null fields are ignored)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateDTO {

    @Schema(description = "Product name", example = "Wireless Headphones Pro")
    private String name;

    @Schema(description = "Product description")
    private String description;

    @Schema(description = "Price in USD", example = "129.99")
    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    @Schema(description = "Available stock", example = "30")
    @PositiveOrZero(message = "Stock cannot be negative")
    private Integer stock;

    @Schema(description = "Category IDs to assign")
    private Set<Long> categoryIds;
}
