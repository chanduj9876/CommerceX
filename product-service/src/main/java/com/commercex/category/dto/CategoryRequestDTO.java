package com.commercex.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Schema(description = "Create or replace a category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequestDTO {

    @Schema(description = "Category name", example = "Electronics")
    @NotBlank(message = "Category name is required")
    private String name;

    @Schema(description = "Category description", example = "Consumer electronics and gadgets")
    private String description;
}
