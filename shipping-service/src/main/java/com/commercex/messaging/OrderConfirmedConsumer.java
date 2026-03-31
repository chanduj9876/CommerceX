package com.commercex.messaging;

import com.commercex.notification.service.NotificationService;
import com.commercex.shipping.client.OrderServiceClient;
import com.commercex.shipping.client.UserServiceClient;
import com.commercex.shipping.client.dto.OrderDTO;
import com.commercex.shipping.client.dto.UserDTO;
import com.commercex.shipping.service.ShipmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class OrderConfirmedConsumer {

    private final ShipmentService shipmentService;
    private final OrderServiceClient orderServiceClient;
    private final UserServiceClient userServiceClient;
    private final NotificationService notificationService;

    public OrderConfirmedConsumer(
            ShipmentService shipmentService,
            OrderServiceClient orderServiceClient,
            UserServiceClient userServiceClient,
            @Qualifier("compositeNotification") NotificationService notificationService) {
        this.shipmentService = shipmentService;
        this.orderServiceClient = orderServiceClient;
        this.userServiceClient = userServiceClient;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "order.confirmed.queue")
    public void consume(Map<String, Object> message) {
        Long orderId = ((Number) message.get("orderId")).longValue();
        log.info("[ORDER CONFIRMED] Received order.confirmed event for order {}", orderId);

        try {
            shipmentService.createShipment(orderId);
            log.info("[ORDER CONFIRMED] Shipment created for order {}", orderId);
        } catch (IllegalStateException e) {
            log.warn("[ORDER CONFIRMED] Shipment skipped for order {}: {}", orderId, e.getMessage());
        } catch (Exception e) {
            log.error("[ORDER CONFIRMED] Failed to create shipment for order {}: {}", orderId, e.getMessage());
            return;
        }

        try {
            OrderDTO order = orderServiceClient.getOrder(orderId);
            UserDTO user = userServiceClient.getUser(order.getUserId());
            notificationService.sendOrderConfirmation(user, order);
        } catch (Exception e) {
            log.error("[ORDER CONFIRMED] Failed to send notification for order {}: {}", orderId, e.getMessage());
        }
    }
}
