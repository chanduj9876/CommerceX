package com.commercex.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Schema(description = "Stock adjustment request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockUpdateRequest {

    @Schema(description = "Quantity to add or subtract", example = "10")
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @Schema(description = "ADD or SUBTRACT", example = "ADD")
    @NotNull(message = "Operation is required (ADD or SUBTRACT)")
    private StockOperation operation;

    public enum StockOperation {
        ADD,
        SUBTRACT
    }
}
