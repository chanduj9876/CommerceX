package com.commercex.product.repository;

import com.commercex.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * JPA Specifications for dynamic Product queries.
 *
 * Why Specifications?
 * - Each method returns a reusable filter predicate
 * - They compose with .and() — only non-null filters are applied
 * - Avoids writing a separate repository method for every filter combination
 */
public class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> nameContains(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.equal(root.join("categories").get("id"), categoryId);
        };
    }
}
