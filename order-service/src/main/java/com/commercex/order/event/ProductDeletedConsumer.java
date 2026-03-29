package com.commercex.order.event;

import com.commercex.cart.repository.CartItemRepository;
import com.commercex.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes product.deleted events from RabbitMQ (published by product-service).
 * Removes cart items referencing the deleted product.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDeletedConsumer {

    private final CartItemRepository cartItemRepository;

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_DELETED_QUEUE)
    @Transactional
    public void onProductDeleted(Long productId) {
        log.info("[PRODUCT DELETED] Removing cart items for product {}", productId);
        cartItemRepository.deleteByProductId(productId);
    }
}
