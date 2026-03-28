package com.commercex.user.dto;

import com.commercex.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO for resetting a password using a reset token.
 * The token is sent to the user's email after calling forgot-password.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @StrongPassword
    private String newPassword;
}
