# Auth API Reference

Base URL: `http://localhost:8080`

> Registration, login, forgot-password, and reset-password are **public** — no token required.
> Change-password requires a valid JWT token.

---

### 1. Register

```
POST /api/v1/auth/register
```

**Password requirements:** At least 8 characters, with uppercase, lowercase, digit, and special character (`@#$%^&+=!*?`).

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "Secret@123"
  }'
```

To register as **ADMIN**, add the `role` field:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Admin User",
    "email": "admin@example.com",
    "password": "Admin@123",
    "role": "ADMIN"
  }'
```

> `role` is optional — defaults to `CUSTOMER` if omitted.

**Response:** `201 Created`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

---

### 2. Login

```
POST /api/v1/auth/login
```

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "Secret@123"
  }'
```

**Response:** `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

---

### 3. Change Password (Authenticated)

```
PUT /api/v1/auth/change-password
```

Requires a valid JWT token. The new password must meet the same strength requirements.

```bash
curl -X PUT http://localhost:8080/api/v1/auth/change-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "currentPassword": "Secret@123",
    "newPassword": "NewSecret@456"
  }'
```

**Response:** `200 OK`

```json
{
  "message": "Password changed successfully"
}
```

---

### 4. Forgot Password

```
POST /api/v1/auth/forgot-password
```

Requests a password reset token. The response is always the same whether the email exists or not (prevents email enumeration).

> **Note:** In this development setup, the reset token is logged to the console. In production, it would be sent via email.

```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com"
  }'
```

**Response:** `200 OK`

```json
{
  "message": "If an account with that email exists, a reset link has been sent"
}
```

---

### 5. Reset Password

```
POST /api/v1/auth/reset-password
```

Resets the password using the token from forgot-password. The token expires after 15 minutes.

```bash
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "newPassword": "ResetSecret@789"
  }'
```

**Response:** `200 OK`

```json
{
  "message": "Password has been reset successfully"
}
```

---

### Using the Token

Add the JWT to the `Authorization` header for protected endpoints:

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "name": "MacBook Pro",
    "description": "Apple laptop",
    "price": 2499.99,
    "stock": 10,
    "categoryIds": [1]
  }'
```

---

### Password Policy

All passwords (register, change, reset) must meet:

| Rule | Requirement |
|------|-------------|
| Length | At least 8 characters |
| Uppercase | At least one (A-Z) |
| Lowercase | At least one (a-z) |
| Digit | At least one (0-9) |
| Special char | At least one of `@#$%^&+=!*?` |

---

### Access Rules

| Endpoint | Access |
|----------|--------|
| `POST /api/v1/auth/register` | Public |
| `POST /api/v1/auth/login` | Public |
| `POST /api/v1/auth/forgot-password` | Public |
| `POST /api/v1/auth/reset-password` | Public |
| `PUT /api/v1/auth/change-password` | Authenticated |
| `GET /api/v1/products/**` | Public |
| `GET /api/v1/categories/**` | Public |
| POST/PUT/PATCH/DELETE products | ADMIN only |
| POST/PUT/PATCH/DELETE categories | ADMIN only |

---

### Error Responses

All errors return a consistent JSON format:

```json
{
  "status": 404,
  "message": "Category not found with id: 1",
  "timestamp": "2026-03-24T10:30:00"
}
```

| Status | Scenario |
|--------|----------|
| `400 Bad Request` | Validation failed (blank name, weak password, etc.) / incorrect current password / invalid or expired reset token |
| `401 Unauthorized` | Invalid email or password / missing token |
| `403 Forbidden` | Valid token but insufficient role (e.g., CUSTOMER on admin endpoint) |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Duplicate name or email |

---

## Class Diagram

```mermaid
classDiagram
    direction TB

    class BaseEntity {
        <<abstract>>
        #Long id
        #LocalDateTime createdAt
        #LocalDateTime updatedAt
        #onCreate() void
        #onUpdate() void
    }

    class User {
        -String name
        -String email
        -String password
        -Role role
    }

    class Role {
        <<enumeration>>
        CUSTOMER
        ADMIN
    }

    class PasswordResetToken {
        -Long id
        -String token
        -User user
        -LocalDateTime expiresAt
        +isExpired() boolean
    }

    class AuthController {
        -AuthService authService
        +register(RegisterRequest) AuthResponse
        +login(LoginRequest) AuthResponse
        +changePassword(Authentication, ChangePasswordRequest) void
        +forgotPassword(ForgotPasswordRequest) String
        +resetPassword(ResetPasswordRequest) void
    }

    class AuthService {
        <<interface>>
        +register(RegisterRequest) AuthResponse
        +login(LoginRequest) AuthResponse
        +changePassword(String, ChangePasswordRequest) void
        +forgotPassword(ForgotPasswordRequest) String
        +resetPassword(ResetPasswordRequest) void
    }

    class AuthServiceImpl {
        -UserRepository userRepository
        -PasswordResetTokenRepository resetTokenRepository
        -PasswordEncoder passwordEncoder
        -JwtUtil jwtUtil
        -AuthenticationManager authManager
    }

    class JwtUtil {
        -SecretKey secretKey
        -long expirationMs
        +generateToken(UserDetails) String
        +extractUsername(String) String
        +validateToken(String, String) boolean
    }

    class JwtAuthenticationFilter {
        -JwtUtil jwtUtil
        +doFilterInternal() void
    }

    class SecurityConfig {
        -JwtAuthenticationFilter jwtFilter
        +securityFilterChain() SecurityFilterChain
        +passwordEncoder() PasswordEncoder
    }

    class UserDetailsServiceImpl {
        -UserRepository userRepository
        +loadUserByUsername(String) UserDetails
    }

    class RegisterRequest {
        -String name
        -String email
        -String password
        -String role
    }

    class LoginRequest {
        -String email
        -String password
    }

    class AuthResponse {
        -String token
        -String email
        -String role
    }

    class ChangePasswordRequest {
        -String currentPassword
        -String newPassword
    }

    class ForgotPasswordRequest {
        -String email
    }

    class ResetPasswordRequest {
        -String token
        -String newPassword
    }

    class MessageResponse {
        -String message
    }

    class UserRepository {
        <<interface>>
        +findByEmail(String) Optional~User~
        +existsByEmail(String) boolean
    }

    class PasswordResetTokenRepository {
        <<interface>>
        +findByToken(String) Optional~PasswordResetToken~
        +deleteByUserId(Long) void
    }

    class StrongPassword {
        <<annotation>>
        8+ chars, upper, lower, digit, special
    }

    class StrongPasswordValidator {
        +isValid(String, ConstraintValidatorContext) boolean
    }

    BaseEntity <|-- User
    User --> Role
    PasswordResetToken "*" --> "1" User : user
    AuthController --> AuthService
    AuthService <|.. AuthServiceImpl
    AuthServiceImpl --> UserRepository
    AuthServiceImpl --> PasswordResetTokenRepository
    AuthServiceImpl --> JwtUtil
    SecurityConfig --> JwtAuthenticationFilter
    JwtAuthenticationFilter --> JwtUtil
    UserDetailsServiceImpl --> UserRepository
    UserRepository ..> User
    PasswordResetTokenRepository ..> PasswordResetToken
    StrongPassword --> StrongPasswordValidator
    RegisterRequest ..> StrongPassword : validates password
    AuthController ..> RegisterRequest : receives
    AuthController ..> LoginRequest : receives
    AuthController ..> AuthResponse : returns
    AuthController ..> ChangePasswordRequest : receives
    AuthController ..> ForgotPasswordRequest : receives
    AuthController ..> ResetPasswordRequest : receives
```

---

## Database Tables

### `users`

| Column | Type | Nullable | Unique | Default | Notes |
|--------|------|----------|--------|---------|-------|
| `id` | BIGSERIAL | NO | PK | auto | Primary key |
| `name` | VARCHAR(255) | NO | NO | — | User display name |
| `email` | VARCHAR(255) | NO | YES | — | Login identifier |
| `password` | VARCHAR(255) | NO | NO | — | BCrypt hash |
| `role` | VARCHAR(255) | NO | NO | CUSTOMER | CUSTOMER or ADMIN |
| `created_at` | TIMESTAMP | NO | NO | NOW() | Immutable after insert |
| `updated_at` | TIMESTAMP | NO | NO | NOW() | Auto-refreshed on update |

**Indexes:** Unique on `email`

### `password_reset_tokens`

| Column | Type | Nullable | Unique | FK | Notes |
|--------|------|----------|--------|---|-------|
| `id` | BIGSERIAL | NO | PK | — | Primary key |
| `token` | VARCHAR(255) | NO | YES | — | UUID reset token |
| `user_id` | BIGINT | NO | NO | `users.id` | Token owner |
| `expires_at` | TIMESTAMP | NO | NO | — | 15-minute expiry |

**Indexes:** Unique on `token`

### ER Diagram

```mermaid
erDiagram
    users {
        bigint id PK
        varchar name
        varchar email UK
        varchar password
        varchar role
        timestamp created_at
        timestamp updated_at
    }

    password_reset_tokens {
        bigint id PK
        varchar token UK
        bigint user_id FK
        timestamp expires_at
    }

    users ||--o{ password_reset_tokens : "has reset tokens"
```
