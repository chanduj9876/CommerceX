package com.commercex.cart.entity;

import com.commercex.common.BaseEntity;
import com.commercex.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Shopping cart — one per user.
 *
 * Why @OneToOne with User? Each user has exactly one cart. When they check out,
 * the cart items convert to an Order, and the cart is cleared (not deleted).
 *
 * Why CascadeType.ALL + orphanRemoval? When we remove a CartItem from the list,
 * JPA auto-deletes the row. When we clear the cart, all items are removed.
 *
 * Why ArrayList (not HashSet)? Cart items have a natural insertion order,
 * and we don't need uniqueness checks — a product can appear once per cart
 * (we increase quantity instead of adding duplicates).
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    /**
     * Adds an item to the cart. If the product already exists, increases quantity.
     */
    public void addItem(CartItem item) {
        for (CartItem existing : items) {
            if (existing.getProduct().getId().equals(item.getProduct().getId())) {
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                return;
            }
        }
        item.setCart(this);
        items.add(item);
    }

    /**
     * Removes a specific item from the cart by its ID.
     */
    public void removeItem(Long cartItemId) {
        items.removeIf(item -> item.getId().equals(cartItemId));
    }

    /**
     * Clears all items from the cart (after checkout).
     */
    public void clear() {
        items.clear();
    }
}
