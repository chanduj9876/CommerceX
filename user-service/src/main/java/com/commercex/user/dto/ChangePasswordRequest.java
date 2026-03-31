package com.commercex.user.dto;

import com.commercex.common.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Schema(description = "Change password request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @Schema(description = "Current password", example = "OldPass@1")
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @Schema(description = "New password", example = "NewPass@2")
    @NotBlank(message = "New password is required")
    @StrongPassword
    private String newPassword;
}
