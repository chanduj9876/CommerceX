package com.commercex.shipping.event;

import com.commercex.shipping.entity.ShipmentStatus;
import lombok.Getter;

@Getter
public class ShipmentStatusChangedEvent {

    private final Long shipmentId;
    private final Long orderId;
    private final String trackingId;
    private final ShipmentStatus oldStatus;
    private final ShipmentStatus newStatus;

    public ShipmentStatusChangedEvent(Long shipmentId, Long orderId, String trackingId,
                                       ShipmentStatus oldStatus, ShipmentStatus newStatus) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.trackingId = trackingId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}
