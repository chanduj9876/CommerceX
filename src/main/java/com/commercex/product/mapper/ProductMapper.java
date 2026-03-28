package com.commercex.product.mapper;

import com.commercex.category.dto.CategoryResponseDTO;
import com.commercex.category.entity.Category;
import com.commercex.product.dto.ProductRequestDTO;
import com.commercex.product.dto.ProductResponseDTO;
import com.commercex.product.entity.Product;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper — converts between Entity and DTO.
 *
 * Why a mapper?
 * - Keeps conversion logic in one place (DRY — Don't Repeat Yourself)
 * - Controller and Service don't need to know how the mapping works
 * - Easy to change if entity or DTO structure changes
 *
 * Using static methods here for simplicity. For larger projects, consider MapStruct
 * (generates mapping code at compile time — zero runtime overhead).
 */
public class ProductMapper {

    private ProductMapper() {
        // Utility class — prevent instantiation
    }

    /**
     * Converts a request DTO to an entity.
     * Note: categories are NOT set here — the service layer handles that
     * because it needs to look up Category entities by ID from the database.
     */
    public static Product toEntity(ProductRequestDTO dto) {
        return Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .build();
    }

    /**
     * Converts an entity to a response DTO.
     * Categories are mapped to CategoryResponseDTOs so we don't expose raw entities.
     */
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
