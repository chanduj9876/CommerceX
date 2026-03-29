package com.commercex.product.mapper;

import com.commercex.category.dto.CategoryResponseDTO;
import com.commercex.category.entity.Category;
import com.commercex.product.dto.ProductRequestDTO;
import com.commercex.product.dto.ProductResponseDTO;
import com.commercex.product.entity.Product;

import java.util.Set;
import java.util.stream.Collectors;

public class ProductMapper {

    private ProductMapper() {
    }

    public static Product toEntity(ProductRequestDTO dto) {
        return Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .build();
    }

    public static ProductResponseDTO toResponseDTO(Product product) {
        Set<CategoryResponseDTO> categoryDTOs = product.getCategories().stream()
                .map(ProductMapper::toCategoryResponseDTO)
                .collect(Collectors.toSet());

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .categories(categoryDTOs)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private static CategoryResponseDTO toCategoryResponseDTO(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
