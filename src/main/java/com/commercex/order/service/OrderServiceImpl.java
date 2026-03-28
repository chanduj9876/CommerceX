package com.commercex.order.service;

import com.commercex.cart.entity.Cart;
import com.commercex.cart.entity.CartItem;
import com.commercex.cart.repository.CartRepository;
import com.commercex.common.EmptyCartException;
import com.commercex.common.InsufficientStockException;
import com.commercex.common.InvalidOrderStateException;
import com.commercex.common.InvalidPromoCodeException;
import com.commercex.common.ResourceNotFoundException;
import com.commercex.order.discount.DiscountStrategy;
import com.commercex.order.discount.DiscountStrategyFactory;
import com.commercex.order.dto.CreateOrderRequest;
import com.commercex.order.dto.OrderResponseDTO;
import com.commercex.order.entity.Order;
import com.commercex.order.entity.OrderItem;
import com.commercex.order.entity.OrderStatus;
import com.commercex.order.event.OrderEventPublisher;
import com.commercex.order.mapper.OrderMapper;
import com.commercex.order.repository.OrderRepository;
import com.commercex.product.entity.Product;
import com.commercex.product.repository.ProductRepository;
import com.commercex.user.entity.User;
import com.commercex.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Order service implementation.
 *
 * createOrder flow (all within a single @Transactional):
 * 1. Load the user's cart with items
 * 2. Validate cart is not empty
 * 3. Validate promo code (if any)
 * 4. For each cart item:
 *    a. Re-fetch the product (ensures latest price and stock)
 *    b. Check stock >= requested quantity
 *    c. Deduct stock from product
 *    d. Snapshot product data into an OrderItem
 * 5. Calculate subtotal from order items
 * 6. Apply discount strategy (percentage, flat, or none)
 * 7. Save the order
 * 8. Clear the cart
 * 9. Publish OrderStatusChangedEvent
 *
 * Why @Transactional on createOrder?
 * If ANY step fails (e.g., insufficient stock on the 3rd item), the entire
 * operation rolls back — no stock deducted, no order created, cart unchanged.
 *
 * Status transition rules:
 *   PENDING → CONFIRMED (payment received)
 *   PENDING → CANCELLED (customer or admin)
 *   CONFIRMED → SHIPPED
 *   CONFIRMED → CANCELLED (before shipping)
 *   SHIPPED → DELIVERED
 *   DELIVERED/CANCELLED → nothing (terminal states)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DiscountStrategyFactory discountStrategyFactory;
    private final OrderEventPublisher orderEventPublisher;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(Long userId, CreateOrderRequest request) {
        // 1. Load cart
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new EmptyCartException("Cart is empty — add items before checkout"));

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cart is empty — add items before checkout");
        }

        // 2. Validate promo code
        String promoCode = request != null ? request.getPromoCode() : null;
        if (promoCode != null && !promoCode.isBlank() && !discountStrategyFactory.isValidPromoCode(promoCode)) {
            throw new InvalidPromoCodeException("Invalid promo code: " + promoCode);
        }

        // 3. Build order
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = Order.builder()
                .user(user)
                .promoCode(promoCode)
                .build();

        // 4. Convert cart items → order items (with stock checks)
        // Batch-fetch all products in one query instead of N individual queries (N+1 fix)
        Set<Long> productIds = cart.getItems().stream()
                .map(cartItem -> cartItem.getProduct().getId())
                .collect(Collectors.toSet());
        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = productMap.get(cartItem.getProduct().getId());
            if (product == null) {
                throw new ResourceNotFoundException(
                        "Product no longer available: " + cartItem.getProduct().getName());
            }

            if (product.getStock() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for '" + product.getName() +
                                "'. Available: " + product.getStock() +
                                ", requested: " + cartItem.getQuantity());
            }

            // Deduct stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            // Snapshot product data into order item
            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();

            order.addItem(orderItem);
            subtotal = subtotal.add(orderItem.getSubtotal());
        }

        // 5. Apply discount
        DiscountStrategy discountStrategy = discountStrategyFactory.getStrategy(promoCode);
        BigDecimal discountAmount = discountStrategy.calculateDiscount(subtotal);
        BigDecimal totalAmount = subtotal.subtract(discountAmount);

        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);

        // 6. Save order
        Order savedOrder = orderRepository.save(order);

        // 7. Clear cart
        cart.clear();
        cartRepository.save(cart);

        // 8. Publish event
        orderEventPublisher.publishOrderCreated(savedOrder.getId(), OrderStatus.PENDING);

        log.info("Order {} created for user {} — subtotal: {}, discount: {}, total: {}",
                savedOrder.getId(), user.getEmail(), subtotal, discountAmount, totalAmount);

        return OrderMapper.toResponseDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Verify ownership: customer can only view their own orders
        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        return OrderMapper.toResponseDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return OrderMapper.toResponseDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getMyOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(OrderMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(OrderStatus status, Pageable pageable) {
        if (status != null) {
            return orderRepository.findByStatus(status, pageable)
                    .map(OrderMapper::toResponseDTO);
        }
        return orderRepository.findAll(pageable)
                .map(OrderMapper::toResponseDTO);
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateStatusTransition(order.getStatus(), newStatus);

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);

        // If cancelling, restore stock
        if (newStatus == OrderStatus.CANCELLED) {
            restoreStock(order);
        }

        Order saved = orderRepository.save(order);
        orderEventPublisher.publishStatusChanged(orderId, oldStatus, newStatus);

        log.info("Order {} status changed: {} → {}", orderId, oldStatus, newStatus);
        return OrderMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public OrderResponseDTO cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Verify the order belongs to this user
        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Only PENDING orders can be cancelled by the customer. Current status: " + order.getStatus());
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        restoreStock(order);

        Order saved = orderRepository.save(order);
        orderEventPublisher.publishStatusChanged(orderId, oldStatus, OrderStatus.CANCELLED);

        log.info("Order {} cancelled by user {}", orderId, userId);
        return OrderMapper.toResponseDTO(saved);
    }

    /**
     * Validates that the status transition is allowed.
     *
     * Valid transitions:
     *   PENDING → CONFIRMED, CANCELLED
     *   CONFIRMED → SHIPPED, CANCELLED
     *   SHIPPED → DELIVERED
     *   DELIVERED → (none — terminal)
     *   CANCELLED → (none — terminal)
     */
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!valid) {
            throw new InvalidOrderStateException(
                    "Cannot transition from " + current + " to " + next);
        }
    }

    /**
     * Restores stock for all items in a cancelled order.
     */
    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
                log.info("Restored {} units of '{}' (product {})",
                        item.getQuantity(), item.getProductName(), item.getProductId());
            } else {
                log.warn("Cannot restore stock for deleted product {} ('{}'). {} units lost.",
                        item.getProductId(), item.getProductName(), item.getQuantity());
            }
        }
    }
}
