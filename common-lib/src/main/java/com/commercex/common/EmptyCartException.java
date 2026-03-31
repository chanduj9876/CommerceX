package com.commercex.common;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation is attempted on an empty cart (e.g., checkout with no items).
 * Returns HTTP 400 Bad Request.
 */
public class EmptyCartException extends BusinessException {

    public EmptyCartException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
