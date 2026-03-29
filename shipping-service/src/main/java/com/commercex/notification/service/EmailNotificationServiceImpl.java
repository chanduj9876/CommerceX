package com.commercex.notification.service;

import com.commercex.shipping.client.dto.OrderDTO;
import com.commercex.shipping.client.dto.UserDTO;
import com.commercex.shipping.entity.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Qualifier("emailNotification")
public class EmailNotificationServiceImpl implements NotificationService {

    @Override
    public void sendOrderConfirmation(UserDTO user, OrderDTO order) {
        log.info("""
                [EMAIL] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                To:      {}
                Subject: Order Confirmed — #{}
                Body:    Hi {}, your order #{} has been confirmed!
                         Total: ${}
                         Thank you for shopping with CommerceX!
                [EMAIL] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━""",
                user.getEmail(), order.getId(), user.getName(),
                order.getId(), order.getTotalAmount());
    }

    @Override
    public void sendShipmentUpdate(UserDTO user, Shipment shipment) {
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
