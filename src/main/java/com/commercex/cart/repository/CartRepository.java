package com.commercex.cart.repository;

import com.commercex.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repository for Cart entity.
 *
 * Why JOIN FETCH? Avoids N+1 queries — loads cart + items + products in one query
 * instead of firing separate SELECT for each item and each product.
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.product WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithItems(Long userId);

    Optional<Cart> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
