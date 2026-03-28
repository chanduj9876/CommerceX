package com.commercex.product.service;

import com.commercex.product.dto.ProductRequestDTO;
import com.commercex.product.dto.ProductResponseDTO;
import com.commercex.product.dto.ProductUpdateDTO;
import com.commercex.product.dto.StockUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product service interface — defines the business operations for products.
 *
 * Why an interface?
 * - Controller depends on this abstraction, not the concrete implementation
 * - Makes unit testing easy — you can mock this interface
 * - Follows the Dependency Inversion Principle (D in SOLID)
 * - Could swap implementations later (e.g., a cached version) without touching the controller
 */
public interface ProductService {

    ProductResponseDTO createProduct(ProductRequestDTO requestDTO);

    ProductResponseDTO getProductById(Long id);

    List<ProductResponseDTO> getAllProducts();

    Page<ProductResponseDTO> getAllProducts(Pageable pageable);

    Page<ProductResponseDTO> searchProducts(String name, BigDecimal minPrice, BigDecimal maxPrice,
                                            Long categoryId, Pageable pageable);

    Page<ProductResponseDTO> getProductsByCategoryId(Long categoryId, Pageable pageable);

    ProductResponseDTO updateProduct(Long id, ProductRequestDTO requestDTO);

    ProductResponseDTO patchProduct(Long id, ProductUpdateDTO updateDTO);

    ProductResponseDTO updateStock(Long id, StockUpdateRequest request);

    void deleteProduct(Long id);
}
