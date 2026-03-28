package com.commercex.notification.service;

import com.commercex.order.entity.Order;
import com.commercex.shipping.entity.Shipment;
import com.commercex.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Qualifier("emailNotification")
public class EmailNotificationServiceImpl implements NotificationService {

    @Override
    public void sendOrderConfirmation(User user, Order order) {
        log.info("""
                [EMAIL] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                To:      {}
                Subject: Order Confirmed — #{}
                Body:    Hi {}, your order #{} has been confirmed!
                         Total: ${}
                         Items: {} item(s)
                         Thank you for shopping with CommerceX!
                [EMAIL] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━""",
                user.getEmail(), order.getId(), user.getName(),
                order.getId(), order.getTotalAmount(), order.getItems().size());
    }

    @Override
    public void sendShipmentUpdate(User user, Shipment shipment) {
        log.info("""
                [EMAIL] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                To:      {}
                Subject: Shipment Update — Tracking #{}
                Body:    Hi {}, your shipment status is now: {}
                         Tracking ID: {}
                         Estimated Delivery: {}
                [EMAIL] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━""",
                user.getEmail(), shipment.getTrackingId(), user.getName(),
                shipment.getStatus(), shipment.getTrackingId(), shipment.getEstimatedDelivery());
    }
}
