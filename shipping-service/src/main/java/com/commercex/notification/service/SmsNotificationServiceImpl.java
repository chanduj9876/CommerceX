package com.commercex.notification.service;

import com.commercex.shipping.client.dto.OrderDTO;
import com.commercex.shipping.client.dto.UserDTO;
import com.commercex.shipping.entity.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Qualifier("smsNotification")
public class SmsNotificationServiceImpl implements NotificationService {

    @Override
    public void sendOrderConfirmation(UserDTO user, OrderDTO order) {
        log.info("""
                [SMS] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                To:      {} (phone placeholder)
                Message: CommerceX: Your order #{} is confirmed! Total: ${}. Thank you!
                [SMS] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━""",
                user.getName(), order.getId(), order.getTotalAmount());
    }

    @Override
    public void sendShipmentUpdate(UserDTO user, Shipment shipment) {
        log.info("""
                [SMS] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                To:      {} (phone placeholder)
                Message: CommerceX: Shipment update — status: {}. Track: {}. ETA: {}
                [SMS] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━""",
                user.getName(), shipment.getStatus(), shipment.getTrackingId(),
                shipment.getEstimatedDelivery());
    }
}
