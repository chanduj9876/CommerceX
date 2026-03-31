package com.commercex.category.mapper;

import com.commercex.category.dto.CategoryRequestDTO;
import com.commercex.category.dto.CategoryResponseDTO;
import com.commercex.category.entity.Category;

public class CategoryMapper {

    private CategoryMapper() {
    }

    public static Category toEntity(CategoryRequestDTO dto) {
        return Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
    }

    public static CategoryResponseDTO toResponseDTO(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
