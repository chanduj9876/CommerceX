package com.commercex.product.service;

import com.commercex.cart.repository.CartItemRepository;
import com.commercex.category.entity.Category;
import com.commercex.category.service.CategoryService;
import com.commercex.common.DuplicateResourceException;
import com.commercex.common.ResourceNotFoundException;
import com.commercex.product.dto.ProductRequestDTO;
import com.commercex.product.dto.ProductResponseDTO;
import com.commercex.product.dto.ProductUpdateDTO;
import com.commercex.product.dto.StockUpdateRequest;
import com.commercex.product.entity.Product;
import com.commercex.product.mapper.ProductMapper;
import com.commercex.product.repository.ProductRepository;
import com.commercex.product.repository.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Product service implementation — contains all the business logic for products.
 *
 * Why @Service? Marks this as a Spring-managed bean. Spring auto-detects it and makes it
 * available for dependency injection wherever ProductService is needed.
 *
 * Why @Transactional on write methods? Ensures that if anything fails mid-operation
 * (e.g., DB constraint violation after partial save), ALL changes are rolled back.
 * The database stays consistent — no half-saved data.
 *
 * Why @RequiredArgsConstructor? Lombok generates a constructor with all final fields.
 * Spring uses this constructor for dependency injection — cleaner than @Autowired on fields.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponseDTO createProduct(ProductRequestDTO requestDTO) {
        log.info("[CACHE EVICT] Creating product — evicting all product cache entries");
        if (productRepository.existsByName(requestDTO.getName())) {
            throw new DuplicateResourceException(
                    "Product already exists with name: " + requestDTO.getName());
        }

        Product product = ProductMapper.toEntity(requestDTO);
        product.setCategories(resolveCategories(requestDTO.getCategoryIds()));

        Product saved = productRepository.save(product);
        return ProductMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductResponseDTO getProductById(Long id) {
        log.info("[CACHE MISS] Product {} not in cache — loading from DB", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));
        return ProductMapper.toResponseDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> searchProducts(String name, BigDecimal minPrice,
                                                   BigDecimal maxPrice, Long categoryId,
                                                   Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();

        if (name != null && !name.isBlank()) {
            spec = spec.and(ProductSpecification.nameContains(name));
        }
        if (minPrice != null) {
            spec = spec.and(ProductSpecification.priceGreaterThanOrEqual(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecification.priceLessThanOrEqual(maxPrice));
        }
        if (categoryId != null) {
            spec = spec.and(ProductSpecification.hasCategoryId(categoryId));
        }

        return productRepository.findAll(spec, pageable)
                .map(ProductMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getProductsByCategoryId(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable)
                .map(ProductMapper::toResponseDTO);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO requestDTO) {
        log.info("[CACHE EVICT] Updating product {} — evicting cache entry", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        // Check if another product with the same name exists
        productRepository.findByName(requestDTO.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Product already exists with name: " + requestDTO.getName());
                });

        product.setName(requestDTO.getName());
        product.setDescription(requestDTO.getDescription());
        product.setPrice(requestDTO.getPrice());
        product.setStock(requestDTO.getStock());
        product.setCategories(resolveCategories(requestDTO.getCategoryIds()));

        Product updated = productRepository.save(product);
        return ProductMapper.toResponseDTO(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductResponseDTO updateStock(Long id, StockUpdateRequest request) {
        log.info("[CACHE EVICT] Updating stock for product {} — evicting cache entry", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        int newStock = switch (request.getOperation()) {
            case ADD -> product.getStock() + request.getQuantity();
            case SUBTRACT -> {
                int result = product.getStock() - request.getQuantity();
                if (result < 0) {
                    throw new com.commercex.common.InsufficientStockException(
                            "Cannot subtract " + request.getQuantity() + " from '" + product.getName()
                                    + "'. Current stock: " + product.getStock());
                }
                yield result;
            }
        };

        product.setStock(newStock);
        Product updated = productRepository.save(product);
        return ProductMapper.toResponseDTO(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        log.info("[CACHE EVICT] Deleting product {} — evicting cache entry", id);
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        cartItemRepository.deleteByProductId(id);
        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductResponseDTO patchProduct(Long id, ProductUpdateDTO updateDTO) {
        log.info("[CACHE EVICT] Patching product {} — evicting cache entry", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        if (updateDTO.getName() != null) {
            if (updateDTO.getName().isBlank()) {
                throw new IllegalArgumentException("Product name cannot be blank");
            }
            productRepository.findByName(updateDTO.getName())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException(
                                "Product already exists with name: " + updateDTO.getName());
                    });
            product.setName(updateDTO.getName());
        }

        if (updateDTO.getDescription() != null) {
            product.setDescription(updateDTO.getDescription());
        }

        if (updateDTO.getPrice() != null) {
            product.setPrice(updateDTO.getPrice());
        }

        if (updateDTO.getStock() != null) {
            product.setStock(updateDTO.getStock());
        }

        if (updateDTO.getCategoryIds() != null) {
            product.setCategories(resolveCategories(updateDTO.getCategoryIds()));
        }

        Product updated = productRepository.save(product);
        return ProductMapper.toResponseDTO(updated);
    }

    /**
     * Resolves category IDs to Category entities via CategoryService.
     *
     * Why CategoryService instead of CategoryRepository? SRP and DIP —
     * ProductServiceImpl shouldn't reach into another feature's data layer.
     * It talks to CategoryService (an abstraction), which owns the category logic.
     */
    private Set<Category> resolveCategories(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(categoryService.findAllByIds(categoryIds));
    }
}
