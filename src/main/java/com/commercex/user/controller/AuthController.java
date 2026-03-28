package com.commercex.user.controller;

import com.commercex.user.dto.*;
import com.commercex.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller — handles registration, login, and password management.
 *
 * Public endpoints (no token required):
 *   POST /register, /login, /forgot-password, /reset-password
 *
 * Authenticated endpoints (token required):
 *   PUT /change-password
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PutMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Password changed successfully")
                .build());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        String resetToken = authService.forgotPassword(request);
        // DEV: Token included in response for Postman testing.
        // PRODUCTION: Remove token from response; send via email instead.
        String message = resetToken != null
                ? "Reset token (dev only): " + resetToken
                : "If an account with that email exists, a reset link has been sent";
        return ResponseEntity.ok(MessageResponse.builder()
                .message(message)
                .build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Password has been reset successfully")
                .build());
    }
}
