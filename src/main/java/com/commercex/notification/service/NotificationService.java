package com.commercex.notification.service;

import com.commercex.order.entity.Order;
import com.commercex.shipping.entity.Shipment;
import com.commercex.user.entity.User;

public interface NotificationService {

    void sendOrderConfirmation(User user, Order order);

    void sendShipmentUpdate(User user, Shipment shipment);
}
