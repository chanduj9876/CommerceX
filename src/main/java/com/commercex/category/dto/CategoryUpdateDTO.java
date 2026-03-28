package com.commercex.category.dto;

import lombok.*;

/**
 * DTO for partial updates (PATCH) — all fields are optional.
 *
 * Why a separate DTO from CategoryRequestDTO?
 * - CREATE needs @NotBlank on name (you can't create a category without a name)
 * - UPDATE should let you change just the description, or just the name, or both
 * - Using the same DTO for both forces the client to resend ALL fields even if
 *   they only want to change one — bad API design
 *
 * Why no validation annotations? Because null means "don't change this field."
 * If the client sends name=null, we keep the existing name. If they send name="",
 * we reject it in the service layer (empty string is not a valid name).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryUpdateDTO {

    private String name;
    private String description;
}
