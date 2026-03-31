package com.commercex.product.controller;

import com.commercex.common.InsufficientStockException;
import com.commercex.common.ResourceNotFoundException;
import com.commercex.product.dto.ProductResponseDTO;
import com.commercex.product.entity.Product;
import com.commercex.product.mapper.ProductMapper;
import com.commercex.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductRepository productRepository;

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ProductResponseDTO> getProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));
        return ResponseEntity.ok(ProductMapper.toResponseDTO(product));
    }

    @PutMapping("/{id}/deduct-stock")
    @Transactional
    public ResponseEntity<ProductResponseDTO> deductStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        if (product.getStock() < quantity) {
            throw new InsufficientStockException(
                    "Cannot deduct " + quantity + " from '" + product.getName()
                            + "'. Current stock: " + product.getStock());
        }

        product.setStock(product.getStock() - quantity);
        Product updated = productRepository.save(product);
        log.info("[INTERNAL] Deducted {} stock from product {} (new stock: {})",
                quantity, id, updated.getStock());
        return ResponseEntity.ok(ProductMapper.toResponseDTO(updated));
    }

    @PutMapping("/{id}/restore-stock")
    @Transactional
    public ResponseEntity<ProductResponseDTO> restoreStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        product.setStock(product.getStock() + quantity);
        Product updated = productRepository.save(product);
        log.info("[INTERNAL] Restored {} stock to product {} (new stock: {})",
                quantity, id, updated.getStock());
        return ResponseEntity.ok(ProductMapper.toResponseDTO(updated));
    }
}
