package com.commercex.common;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an invalid promo code is used during checkout.
 * Returns HTTP 400 Bad Request.
 */
public class InvalidPromoCodeException extends BusinessException {

    public InvalidPromoCodeException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
