package com.commercex.user.entity;

/**
 * Role enum — defines the access levels in the system.
 *
 * CUSTOMER: Can browse products, manage cart, place orders.
 * ADMIN: Can do everything a customer can, plus manage products and categories.
 *
 * Spring Security expects roles prefixed with "ROLE_" internally,
 * but @PreAuthorize("hasRole('ADMIN')") strips the prefix for you.
 */
public enum Role {
    CUSTOMER,
    ADMIN
}
