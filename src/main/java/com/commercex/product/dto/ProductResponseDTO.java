package com.commercex.product.dto;

import com.commercex.category.dto.CategoryResponseDTO;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO — what the client receives.
 *
 * Includes id and timestamps (server-generated fields).
 * Categories are returned as nested DTOs so the client sees category names, not raw entities.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO implements Serializable {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Set<CategoryResponseDTO> categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
