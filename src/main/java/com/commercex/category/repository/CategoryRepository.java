package com.commercex.category.repository;

import com.commercex.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for Category entity.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    // Page<Category> findAll(Pageable) is inherited from JpaRepository
}
