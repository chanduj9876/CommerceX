package com.commercex.order.service;

import com.commercex.cart.entity.Cart;
import com.commercex.cart.entity.CartItem;
import com.commercex.cart.repository.CartRepository;
import com.commercex.common.EmptyCartException;
import com.commercex.common.InsufficientStockException;
import com.commercex.common.InvalidOrderStateException;
import com.commercex.common.InvalidPromoCodeException;
import com.commercex.common.ResourceNotFoundException;
import com.commercex.order.client.ProductDTO;
import com.commercex.order.client.ProductServiceClient;
import com.commercex.order.client.UserDTO;
import com.commercex.order.client.UserServiceClient;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductServiceClient productServiceClient;
    private final UserServiceClient userServiceClient;
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

        // 3. Fetch user from user-service
        UserDTO user = userServiceClient.getUserById(userId);

        Order order = Order.builder()
                .userId(userId)
                .userEmail(user.getEmail())
                .promoCode(promoCode)
                .build();

        // 4. Convert cart items → order items (with stock deduction via product-service)
        BigDecimal subtotal = BigDecimal.ZERO;
        List<CartItem> deductedItems = new ArrayList<>();

        try {
            for (CartItem cartItem : cart.getItems()) {
                Long productId = cartItem.getProductId();

                // Fetch product from product-service
                ProductDTO product = productServiceClient.getProduct(productId);

                // Deduct stock via product-service (throws InsufficientStockException if not enough)
                productServiceClient.deductStock(productId, cartItem.getQuantity());
                deductedItems.add(cartItem);

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
        } catch (Exception e) {
            // Compensate: restore stock for every item already deducted before the failure
            for (CartItem deducted : deductedItems) {
                try {
                    productServiceClient.restoreStock(deducted.getProductId(), deducted.getQuantity());
                    log.info("[ORDER] Compensated stock for product {} after order creation failure",
                            deducted.getProductId());
                } catch (Exception restoreEx) {
                    log.error("[ORDER] Failed to compensate stock for product {} — {} units lost. Error: {}",
                            deducted.getProductId(), deducted.getQuantity(), restoreEx.getMessage());
                }
            }
            throw e;
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

        if (!order.getUserId().equals(userId)) {
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

        // If cancelling, restore stock via product-service
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

        if (!order.getUserId().equals(userId)) {
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

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            try {
                productServiceClient.restoreStock(item.getProductId(), item.getQuantity());
                log.info("Restored {} units of '{}' (product {})",
                        item.getQuantity(), item.getProductName(), item.getProductId());
            } catch (Exception e) {
                log.warn("Cannot restore stock for product {} ('{}'). {} units lost. Error: {}",
                        item.getProductId(), item.getProductName(), item.getQuantity(), e.getMessage());
            }
        }
    }
}
