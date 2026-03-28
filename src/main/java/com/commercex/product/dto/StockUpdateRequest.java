package com.commercex.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * DTO for updating product stock via the dedicated stock endpoint.
 *
 * Why a separate DTO instead of reusing ProductUpdateDTO?
 * - Stock operations are semantically different: ADD/SUBTRACT vs. absolute set
 * - Prevents accidental changes to other product fields
 * - The operation field makes the intent explicit (no ambiguity)
 *
 * Example: { "quantity": 50, "operation": "ADD" } → adds 50 units to current stock
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockUpdateRequest {

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @NotNull(message = "Operation is required (ADD or SUBTRACT)")
    private StockOperation operation;

    public enum StockOperation {
        ADD,
        SUBTRACT
    }
}
