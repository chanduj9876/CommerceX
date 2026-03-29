package com.commercex.category.service;

import com.commercex.category.dto.CategoryRequestDTO;
import com.commercex.category.dto.CategoryResponseDTO;
import com.commercex.category.dto.CategoryUpdateDTO;
import com.commercex.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface CategoryService {

    CategoryResponseDTO createCategory(CategoryRequestDTO requestDTO);

    CategoryResponseDTO getCategoryById(Long id);

    List<CategoryResponseDTO> getAllCategories();

    Page<CategoryResponseDTO> getAllCategories(Pageable pageable);

    CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO requestDTO);

    CategoryResponseDTO patchCategory(Long id, CategoryUpdateDTO updateDTO);

    void deleteCategory(Long id);

    List<Category> findAllByIds(Set<Long> ids);
}
