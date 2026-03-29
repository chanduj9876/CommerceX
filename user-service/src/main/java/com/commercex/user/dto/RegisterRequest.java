package com.commercex.user.dto;

import com.commercex.common.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Schema(description = "User registration request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @Schema(description = "Full name", example = "Jane Doe")
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(description = "Email address", example = "jane@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Schema(description = "Password (min 8 chars, 1 uppercase, 1 digit, 1 special)", example = "Secure@123")
    @NotBlank(message = "Password is required")
    @StrongPassword
    private String password;

    @Schema(description = "Role: USER or ADMIN (defaults to USER)", example = "USER")
    private String role;
}
