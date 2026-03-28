package com.commercex.shipping.entity;

/**
 * Shipment lifecycle status.
 *
 * State transitions:
 *   PROCESSING → SHIPPED → IN_TRANSIT → DELIVERED
 *
 * PROCESSING:  Shipment created, being prepared for dispatch
 * SHIPPED:     Handed off to carrier
 * IN_TRANSIT:  On the way to the customer
 * DELIVERED:   Customer received the package
 */
public enum ShipmentStatus {
    PROCESSING,
    SHIPPED,
    IN_TRANSIT,
    DELIVERED
}
