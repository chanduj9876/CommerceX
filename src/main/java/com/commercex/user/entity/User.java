package com.commercex.user.entity;

import com.commercex.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * User entity — represents a registered user (customer or admin).
 *
 * Why @Email? Validates email format at the bean validation level.
 * Why unique email? Each account must have a unique email — enforced at DB level too.
 * Why @Enumerated(STRING)? Stores "CUSTOMER"/"ADMIN" as text, not ordinal numbers.
 *   If we used ordinals and reordered the enum, existing data would break.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.CUSTOMER;
}
