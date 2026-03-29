package com.commercex.common;

import org.springframework.http.HttpStatus;

/**
 * Abstract base for all business exceptions in the application.
 *
 * Why this base class? Open/Closed Principle — the GlobalExceptionHandler has ONE handler
 * for BusinessException. When you add a new exception (e.g., InsufficientStockException),
 * you just extend BusinessException and set the status. The handler doesn't need to change.
 *
 * Each subclass declares its own HTTP status via getStatus(). The handler reads it
 * and builds the response automatically.
 */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }

    /**
     * Each subclass returns its appropriate HTTP status.
     * ResourceNotFoundException → 404, DuplicateResourceException → 409, etc.
     */
    public abstract HttpStatus getStatus();
}
