package com.commercex.common;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an invalid order state transition is attempted
 * (e.g., cancelling a DELIVERED order, or shipping a CANCELLED order).
 * Returns HTTP 400 Bad Request.
 */
public class InvalidOrderStateException extends BusinessException {

    public InvalidOrderStateException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
