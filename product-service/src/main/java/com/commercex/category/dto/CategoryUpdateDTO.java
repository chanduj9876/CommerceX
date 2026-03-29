package com.commercex.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "Partial category update")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryUpdateDTO {

    @Schema(description = "New category name", example = "Consumer Electronics")
    private String name;

    @Schema(description = "New description")
    private String description;
}
