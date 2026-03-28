package com.commercex.notification.service;

import com.commercex.order.entity.Order;
import com.commercex.shipping.entity.Shipment;
import com.commercex.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Composite pattern — delegates to ALL NotificationService implementations.
 *
 * Why? OrderEventListener and ShipmentEventListener both had to inject two
 * separate @Qualifier beans and call each one manually. This class eliminates
 * that duplication: inject one "compositeNotification" bean and it fans out
 * to email + SMS (and any future channels like push notifications).
 *
 * Adding a new notification channel (e.g., PushNotificationServiceImpl) requires
 * zero changes here — Spring auto-discovers all NotificationService beans.
 */
@Slf4j
@Service
@Qualifier("compositeNotification")
public class CompositeNotificationService implements NotificationService {

    private final List<NotificationService> delegates;

    public CompositeNotificationService(
            @Qualifier("emailNotification") NotificationService emailNotification,
            @Qualifier("smsNotification") NotificationService smsNotification) {
        this.delegates = List.of(emailNotification, smsNotification);
    }

    @Override
    public void sendOrderConfirmation(User user, Order order) {
        for (NotificationService delegate : delegates) {
            try {
                delegate.sendOrderConfirmation(user, order);
            } catch (Exception e) {
                log.error("[NOTIFICATION] Failed to send order confirmation via {}: {}",
                        delegate.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    @Override
    public void sendShipmentUpdate(User user, Shipment shipment) {
        for (NotificationService delegate : delegates) {
            try {
                delegate.sendShipmentUpdate(user, shipment);
            } catch (Exception e) {
                log.error("[NOTIFICATION] Failed to send shipment update via {}: {}",
                        delegate.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}
