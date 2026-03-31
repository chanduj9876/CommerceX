package com.commercex.notification.service;

import com.commercex.shipping.client.dto.OrderDTO;
import com.commercex.shipping.client.dto.UserDTO;
import com.commercex.shipping.entity.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public void sendOrderConfirmation(UserDTO user, OrderDTO order) {
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
    public void sendShipmentUpdate(UserDTO user, Shipment shipment) {
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
