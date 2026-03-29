package com.commercex.user.service;

import com.commercex.common.DuplicateEmailException;
import com.commercex.common.InvalidPasswordException;
import com.commercex.common.ResourceNotFoundException;
import com.commercex.security.JwtUtil;
import com.commercex.user.dto.*;
import com.commercex.user.entity.PasswordResetToken;
import com.commercex.user.entity.Role;
import com.commercex.user.entity.User;
import com.commercex.user.repository.PasswordResetTokenRepository;
import com.commercex.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int RESET_TOKEN_EXPIRY_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(
                    "Email already registered: " + email);
        }

        User user = User.builder()
                .name(request.getName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(resolveRole(request.getRole()))
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(buildUserDetails(user));

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        String token = jwtUtil.generateToken(buildUserDetails(user));

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase();

        return userRepository.findByEmail(email).map(user -> {
            resetTokenRepository.deleteByUserId(user.getId());

            String token = UUID.randomUUID().toString();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES))
                    .build();

            resetTokenRepository.save(resetToken);

            log.info("Password reset token for {}: {}", email, token);
            return token;
        }).orElse(null);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidPasswordException("Invalid reset token"));

        if (resetToken.isExpired()) {
            resetTokenRepository.delete(resetToken);
            throw new InvalidPasswordException("Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetTokenRepository.deleteByUserId(user.getId());
    }

    private UserDetails buildUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    private Role resolveRole(String role) {
        if (role == null || role.isBlank()) {
            return Role.CUSTOMER;
        }
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Role.CUSTOMER;
        }
    }
}
