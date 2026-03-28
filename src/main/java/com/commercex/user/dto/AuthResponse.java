package com.commercex.user.dto;

import lombok.*;

/**
 * DTO returned after successful login or registration.
 *
 * Contains the JWT token the client must include in subsequent requests
 * as: Authorization: Bearer <token>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String email;
    private String role;
}
