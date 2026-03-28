package com.commercex.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Base entity — every JPA entity extends this to inherit id + timestamp fields.
 *
 * Why @MappedSuperclass? Tells JPA "this isn't a table itself, but its fields should be
 * inherited by child entities." Product, Category, and every future entity (Order, User, etc.)
 * automatically get id, createdAt, updatedAt without repeating code.
 *
 * Why extract this? Before, both Product and Category had identical id/timestamp fields
 * and @PrePersist/@PreUpdate hooks — pure duplication. If we ever need to add a
 * "createdBy" audit field, we change ONE class instead of every entity.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
