package com.commercex.common;

import org.springframework.http.HttpStatus;

/**
 * Thrown when trying to create a resource that already exists (e.g., duplicate product name).
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
