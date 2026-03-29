package com.commercex.common;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a password operation fails (wrong current password, invalid/expired reset token).
 * Returns HTTP 400 Bad Request.
 */
public class InvalidPasswordException extends BusinessException {

    public InvalidPasswordException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
