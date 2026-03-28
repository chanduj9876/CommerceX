package com.commercex.common;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a user tries to register with an email that already exists.
 * Returns HTTP 409 Conflict.
 */
public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
