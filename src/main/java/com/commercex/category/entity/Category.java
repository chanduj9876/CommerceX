package com.commercex.category.entity;

import com.commercex.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Category entity — products can be grouped into categories.
 *
 * Why a separate entity (not just a String tag)? Because categories have their own identity,
 * can be managed independently (CRUD), and we can enforce uniqueness at the DB level.
 * A join table (product_categories) handles the many-to-many relationship.
 *
 * Why extends BaseEntity? Inherits id, createdAt, updatedAt, and lifecycle hooks.
 * Previously Category only had createdAt — now it also tracks updatedAt for audit consistency.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    @NotBlank(message = "Category name is required")
    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    /**
     * Inverse side of the Product↔Category many-to-many.
     * mappedBy = "categories" points to the Product.categories field (the owning side).
     * This allows us to navigate from Category → Products when needed (e.g., cascade delete cleanup).
     */
    @ManyToMany(mappedBy = "categories")
    @Builder.Default
    private Set<com.commercex.product.entity.Product> products = new HashSet<>();
}
