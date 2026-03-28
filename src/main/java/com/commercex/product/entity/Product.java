package com.commercex.product.entity;

import com.commercex.category.entity.Category;
import com.commercex.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Product entity — represents a product in the e-commerce catalog.
 *
 * Why BigDecimal for price? double has floating-point precision errors
 * (0.1 + 0.2 = 0.30000000000000004). BigDecimal gives exact arithmetic — critical for money.
 *
 * Why extends BaseEntity? Inherits id, createdAt, updatedAt, and lifecycle hooks from
 * the common base class — no duplication across entities.
 *
 * Why @ManyToMany? A product can belong to multiple categories, and a category can have multiple products.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @NotBlank(message = "Product name is required")
    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Positive(message = "Price must be greater than zero")
    @Column(nullable = false)
    private BigDecimal price;

    @PositiveOrZero(message = "Stock cannot be negative")
    @Column(nullable = false)
    private Integer stock;

    /**
     * Optimistic locking — prevents concurrent stock overwrites.
     * JPA auto-increments this on every UPDATE. If two transactions read the
     * same version and both try to write, the second one gets an
     * OptimisticLockException → the caller can retry or fail gracefully.
     */
    @Version
    private Long version = 0L;

    @ManyToMany
    @JoinTable(
            name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();
}
