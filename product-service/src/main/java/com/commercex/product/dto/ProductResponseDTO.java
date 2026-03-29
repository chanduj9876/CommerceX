package com.commercex.product.dto;

import com.commercex.category.dto.CategoryResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "Product details response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO implements Serializable {

    @Schema(description = "Product ID", example = "1")
    private Long id;
    @Schema(description = "Product name", example = "Wireless Headphones")
    private String name;
    @Schema(description = "Product description")
    private String description;
    @Schema(description = "Price in USD", example = "99.99")
    private BigDecimal price;
    @Schema(description = "Available stock", example = "50")
    private Integer stock;
    @Schema(description = "Assigned categories")
    private Set<CategoryResponseDTO> categories;
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
