package com.commercex.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service", path = "/internal/products")
public interface ProductServiceClient {

    @GetMapping("/{id}")
    ProductDTO getProduct(@PathVariable("id") Long id);

    @PutMapping("/{id}/deduct-stock")
    ProductDTO deductStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);

    @PutMapping("/{id}/restore-stock")
    ProductDTO restoreStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);
}
