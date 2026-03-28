package com.commercex.category.service;

import com.commercex.category.dto.CategoryRequestDTO;
import com.commercex.category.dto.CategoryResponseDTO;
import com.commercex.category.dto.CategoryUpdateDTO;
import com.commercex.category.entity.Category;
import com.commercex.category.mapper.CategoryMapper;
import com.commercex.category.repository.CategoryRepository;
import com.commercex.common.DuplicateResourceException;
import com.commercex.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponseDTO createCategory(CategoryRequestDTO requestDTO) {
        log.info("[CACHE EVICT] Creating category — evicting all category cache entries");
        if (categoryRepository.existsByName(requestDTO.getName())) {
            throw new DuplicateResourceException(
                    "Category already exists with name: " + requestDTO.getName());
        }

        Category category = CategoryMapper.toEntity(requestDTO);
        Category saved = categoryRepository.save(category);
        return CategoryMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#id")
    public CategoryResponseDTO getCategoryById(Long id) {
        log.info("[CACHE MISS] Category {} not in cache — loading from DB", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + id));
        return CategoryMapper.toResponseDTO(category);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponseDTO> getAllCategories() {
        log.info("[CACHE MISS] All categories not in cache — loading from DB");
        return categoryRepository.findAll().stream()
                .map(CategoryMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponseDTO> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable)
                .map(CategoryMapper::toResponseDTO);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO requestDTO) {
        log.info("[CACHE EVICT] Updating category {} — evicting all category cache entries", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + id));

        categoryRepository.findByName(requestDTO.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Category already exists with name: " + requestDTO.getName());
                });

        category.setName(requestDTO.getName());
        category.setDescription(requestDTO.getDescription());

        Category updated = categoryRepository.save(category);
        return CategoryMapper.toResponseDTO(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponseDTO patchCategory(Long id, CategoryUpdateDTO updateDTO) {
        log.info("[CACHE EVICT] Patching category {} — evicting all category cache entries", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + id));

        if (updateDTO.getName() != null) {
            if (updateDTO.getName().isBlank()) {
                throw new IllegalArgumentException("Category name cannot be blank");
            }
            categoryRepository.findByName(updateDTO.getName())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException(
                                "Category already exists with name: " + updateDTO.getName());
                    });
            category.setName(updateDTO.getName());
        }

        if (updateDTO.getDescription() != null) {
            category.setDescription(updateDTO.getDescription());
        }

        Category updated = categoryRepository.save(category);
        return CategoryMapper.toResponseDTO(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        log.info("[CACHE EVICT] Deleting category {} — evicting all category cache entries", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + id));

        // Remove this category from all products' category sets (cleans up join table)
        for (var product : category.getProducts()) {
            product.getCategories().remove(category);
        }

        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAllByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Category> categories = categoryRepository.findAllById(ids);
        if (categories.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more category IDs not found");
        }
        return categories;
    }
}
