package com.commercex.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Schema(description = "Category details response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDTO implements Serializable {

    @Schema(description = "Category ID", example = "1")
    private Long id;
    @Schema(description = "Category name", example = "Electronics")
    private String name;
    @Schema(description = "Category description")
    private String description;
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
