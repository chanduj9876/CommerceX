package com.commercex.product.service;

import com.commercex.product.dto.ProductRequestDTO;
import com.commercex.product.dto.ProductResponseDTO;
import com.commercex.product.dto.ProductUpdateDTO;
import com.commercex.product.dto.StockUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

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
