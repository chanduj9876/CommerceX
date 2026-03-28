package com.commercex.category.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Response DTO for category — returned to the client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDTO implements Serializable {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
