package com.commercex.user.dto;

import com.commercex.common.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO for user registration.
 *
 * Password must meet strength requirements: 8+ chars, uppercase, lowercase, digit, special char.
 * The raw password is validated here; it's BCrypt-hashed before saving.
 *
 * Role is optional — defaults to CUSTOMER if not provided.
 * Pass "role": "ADMIN" to create an admin account.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @StrongPassword
    private String password;

    private String role;
}
