package com.commercex.product.repository;

import com.commercex.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository for Product entity.
 *
 * Why extend JpaRepository?
 * - Gives you CRUD operations for free: save(), findById(), findAll(), deleteById(), etc.
 * - Also provides pagination and sorting out of the box via PagingAndSortingRepository
 * - Spring auto-generates the implementation at runtime — no boilerplate code needed
 *
 * Why JpaSpecificationExecutor?
 * - Enables dynamic query building for search/filter use cases
 * - Avoids writing many custom query methods for every filter combination
 *
 * Why findByName returns Optional?
 * - Forces the caller to handle the "not found" case explicitly
 * - Avoids NullPointerException — a common source of bugs
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT p FROM Product p JOIN p.categories c WHERE c.id = :categoryId")
    Page<Product> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);
}
