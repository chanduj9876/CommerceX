package com.commercex.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "Authentication response with JWT token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    @Schema(description = "JWT bearer token", example = "eyJhbGci...")
    private String token;

    @Schema(description = "Authenticated user email", example = "jane@example.com")
    private String email;

    @Schema(description = "User role", example = "USER")
    private String role;
}
