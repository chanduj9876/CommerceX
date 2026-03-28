package com.commercex.notification.service;

import com.commercex.order.entity.Order;
import com.commercex.shipping.entity.Shipment;
import com.commercex.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Qualifier("smsNotification")
public class SmsNotificationServiceImpl implements NotificationService {

    @Override
    public void sendOrderConfirmation(User user, Order order) {
        log.info("""
                [SMS] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                To:      {} (phone placeholder)
                Message: CommerceX: Your order #{} is confirmed! Total: ${}. Thank you!
                [SMS] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━""",
                user.getName(), order.getId(), order.getTotalAmount());
    }

    @Override
    public void sendShipmentUpdate(User user, Shipment shipment) {
        log.info("""
                [SMS] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                To:      {} (phone placeholder)
                Message: CommerceX: Shipment update — status: {}. Track: {}. ETA: {}
                [SMS] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━""",
                user.getName(), shipment.getStatus(), shipment.getTrackingId(),
                shipment.getEstimatedDelivery());
    }
}
