package com.commercex.product.service;

import com.commercex.common.InsufficientStockException;
import com.commercex.common.ResourceNotFoundException;
import com.commercex.product.dto.ProductRequestDTO;
import com.commercex.product.dto.ProductResponseDTO;
import com.commercex.product.dto.StockUpdateRequest;
import com.commercex.product.dto.StockUpdateRequest.StockOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for stock management and cache invalidation.
 *
 * Uses local PostgreSQL (commercex_test DB) and Memurai/Redis.
 * The "integration" profile loads application-integration.properties
 * which points to a dedicated test database with create-drop DDL mode.
 *
 * These tests verify:
 * - Stock deduction via updateStock (ADD/SUBTRACT)
 * - InsufficientStockException when subtracting more than available
 * - @Cacheable populates the cache on getProductById
 * - @CacheEvict clears the cache on updateStock, updateProduct, deleteProduct
 */
@SpringBootTest
@ActiveProfiles("integration")
class ProductStockCacheIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        var cache = cacheManager.getCache("products");
        if (cache != null) {
            cache.clear();
        }
    }

    private ProductResponseDTO createTestProduct(String name, int stock) {
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name(name)
                .description("Test product")
                .price(new BigDecimal("99.99"))
                .stock(stock)
                .categoryIds(Set.of())
                .build();
        return productService.createProduct(request);
    }

    // ================================
    // Stock deduction tests
    // ================================
    @Nested
    @DisplayName("Stock Update Operations")
    class StockUpdateTests {

        @Test
        @DisplayName("ADD operation increases stock")
        void addStock() {
            ProductResponseDTO product = createTestProduct("Stock-Add-Test", 10);

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .quantity(25)
                    .operation(StockOperation.ADD)
                    .build();

            ProductResponseDTO updated = productService.updateStock(product.getId(), request);

            assertThat(updated.getStock()).isEqualTo(35);
        }

        @Test
        @DisplayName("SUBTRACT operation decreases stock")
        void subtractStock() {
            ProductResponseDTO product = createTestProduct("Stock-Sub-Test", 50);

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .quantity(20)
                    .operation(StockOperation.SUBTRACT)
                    .build();

            ProductResponseDTO updated = productService.updateStock(product.getId(), request);

            assertThat(updated.getStock()).isEqualTo(30);
        }

        @Test
        @DisplayName("SUBTRACT more than available throws InsufficientStockException")
        void subtractTooMuch_throws() {
            ProductResponseDTO product = createTestProduct("Stock-Overflow-Test", 5);

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .quantity(10)
                    .operation(StockOperation.SUBTRACT)
                    .build();

            assertThatThrownBy(() -> productService.updateStock(product.getId(), request))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Cannot subtract");
        }

        @Test
        @DisplayName("updateStock on non-existent product throws ResourceNotFoundException")
        void updateStock_notFound() {
            StockUpdateRequest request = StockUpdateRequest.builder()
                    .quantity(5)
                    .operation(StockOperation.ADD)
                    .build();

            assertThatThrownBy(() -> productService.updateStock(99999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ================================
    // Cache invalidation tests
    // ================================
    @Nested
    @DisplayName("Cache Behavior")
    class CacheTests {

        @Test
        @DisplayName("getProductById populates the cache")
        void getById_cachesResult() {
            ProductResponseDTO product = createTestProduct("Cache-Hit-Test", 10);

            // First call — hits DB, populates cache
            productService.getProductById(product.getId());

            // Verify cache is populated (RedisCache.get returns ValueWrapper)
            var cache = cacheManager.getCache("products");
            assertThat(cache).isNotNull();
            var wrapper = cache.get(product.getId());
            assertThat(wrapper).as("Cache should contain entry for product %d after getProductById", product.getId()).isNotNull();
        }

        @Test
        @DisplayName("updateStock evicts cache entry")
        void updateStock_evictsCache() {
            ProductResponseDTO product = createTestProduct("Cache-Evict-Stock-Test", 10);

            // Populate cache
            productService.getProductById(product.getId());

            // Update stock — should evict
            StockUpdateRequest request = StockUpdateRequest.builder()
                    .quantity(5)
                    .operation(StockOperation.ADD)
                    .build();
            productService.updateStock(product.getId(), request);

            // Cache should be evicted — re-fetch should return fresh data
            ProductResponseDTO fresh = productService.getProductById(product.getId());
            assertThat(fresh.getStock()).isEqualTo(15);
        }

        @Test
        @DisplayName("updateProduct evicts cache entry and next fetch reflects changes")
        void updateProduct_evictsCache() {
            ProductResponseDTO product = createTestProduct("Cache-Evict-Update-Test", 10);

            // Populate cache
            productService.getProductById(product.getId());

            // Update product — should evict
            ProductRequestDTO updateRequest = ProductRequestDTO.builder()
                    .name("Cache-Evict-Update-Test-Renamed")
                    .description("Updated description")
                    .price(new BigDecimal("149.99"))
                    .stock(10)
                    .categoryIds(Set.of())
                    .build();
            productService.updateProduct(product.getId(), updateRequest);

            // After eviction, next fetch should return updated data (not stale cache)
            ProductResponseDTO fresh = productService.getProductById(product.getId());
            assertThat(fresh.getDescription()).isEqualTo("Updated description");
            assertThat(fresh.getPrice()).isEqualByComparingTo("149.99");
        }

        @Test
        @DisplayName("deleteProduct evicts cache entry")
        void deleteProduct_evictsCache() {
            ProductResponseDTO product = createTestProduct("Cache-Evict-Delete-Test", 10);

            // Populate cache
            productService.getProductById(product.getId());

            // Delete product — should evict
            productService.deleteProduct(product.getId());

            // After deletion, fetching should throw (not return stale cached data)
            assertThatThrownBy(() -> productService.getProductById(product.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
