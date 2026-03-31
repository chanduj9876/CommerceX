package com.commercex.common;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource (product, category, etc.) is not found.
 *
 * Why a custom exception instead of returning null?
 * - Makes the intent explicit — "this resource doesn't exist"
 * - GlobalExceptionHandler catches it via BusinessException and returns a clean 404 response
 * - Avoids scattered null checks and inconsistent error responses across controllers
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
