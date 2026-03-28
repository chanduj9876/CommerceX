package com.commercex.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores password reset tokens.
 *
 * Flow: user requests reset → random token generated → stored here with 15-min expiry.
 * User submits token + new password → token is validated → password updated → token deleted.
 *
 * Why a separate table instead of a column on User?
 * - Tokens are short-lived and should be cleaned up
 * - Keeps the User entity clean
 * - Allows multiple reset requests (only latest is valid)
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
