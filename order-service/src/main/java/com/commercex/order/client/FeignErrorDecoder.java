package com.commercex.order.client;

import com.commercex.common.InsufficientStockException;
import com.commercex.common.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

@Component
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            case 404 -> new ResourceNotFoundException("Remote resource not found (called: " + methodKey + ")");
            case 409 -> new InsufficientStockException("Insufficient stock (called: " + methodKey + ")");
            default -> defaultDecoder.decode(methodKey, response);
        };
    }
}
