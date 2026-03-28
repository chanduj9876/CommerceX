package com.commercex.user.dto;

import com.commercex.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO for changing password (authenticated user).
 * Requires the current password for verification before accepting the new one.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @StrongPassword
    private String newPassword;
}
