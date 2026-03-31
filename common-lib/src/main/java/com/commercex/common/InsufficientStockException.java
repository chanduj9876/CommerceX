package com.commercex.common;

import org.springframework.http.HttpStatus;

/**
 * Thrown when requested quantity exceeds available stock.
 * Returns HTTP 400 Bad Request (cart time) or HTTP 422 Unprocessable Entity (order time).
 */
public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
