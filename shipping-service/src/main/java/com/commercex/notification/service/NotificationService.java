package com.commercex.notification.service;

import com.commercex.shipping.client.dto.OrderDTO;
import com.commercex.shipping.client.dto.UserDTO;
import com.commercex.shipping.entity.Shipment;

public interface NotificationService {

    void sendOrderConfirmation(UserDTO user, OrderDTO order);

    void sendShipmentUpdate(UserDTO user, Shipment shipment);
}
