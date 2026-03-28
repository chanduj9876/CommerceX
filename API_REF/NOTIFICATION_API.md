# Notification Service Reference

## Overview
The Notification module provides simulated email and SMS notifications triggered by order and shipment lifecycle events. All notifications are logged to the console via SLF4J — no real SMTP or SMS gateway is configured.

---

## Notification Triggers

| Event | Notification Sent | Channels |
|-------|-------------------|----------|
| Order status → CONFIRMED | Order confirmation | Email + SMS |
| Shipment status changes | Shipment update | Email + SMS |

---

## Architecture

### NotificationService Interface
```java
public interface NotificationService {
    void sendOrderConfirmation(User user, Order order);
    void sendShipmentUpdate(User user, Shipment shipment);
}
```

### Implementations

| Implementation | Qualifier | Description |
|----------------|-----------|-------------|
| `EmailNotificationServiceImpl` | `@Qualifier("emailNotification")` | Formats and logs email content (To, Subject, Body) |
| `SmsNotificationServiceImpl` | `@Qualifier("smsNotification")` | Formats and logs SMS content (Recipient, Message) |

---

## Event Wiring

### OrderEventListener
Listens to `OrderStatusChangedEvent`. When `newStatus == CONFIRMED`:
1. Auto-creates a shipment via `ShipmentService.createShipment(orderId)`
2. Sends order confirmation via both email and SMS notification services

### ShipmentEventListener
Listens to `ShipmentStatusChangedEvent`. On every status change:
1. Sends shipment update via both email and SMS notification services

---

## Sample Log Output

### Email — Order Confirmation
```
[EMAIL] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
To:      john@example.com
Subject: Order Confirmed — #42
Body:    Hi John, your order #42 has been confirmed!
         Total: $149.99
         Items: 3 item(s)
         Thank you for shopping with CommerceX!
[EMAIL] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### SMS — Order Confirmation
```
[SMS] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
To:      John (phone placeholder)
Message: CommerceX: Your order #42 is confirmed! Total: $149.99. Thank you!
[SMS] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Email — Shipment Update
```
[EMAIL] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
To:      john@example.com
Subject: Shipment Update — Tracking #a1b2c3d4-...
Body:    Hi John, your shipment status is now: SHIPPED
         Tracking ID: a1b2c3d4-...
         Estimated Delivery: 2026-04-04
[EMAIL] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### SMS — Shipment Update
```
[SMS] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
To:      John (phone placeholder)
Message: CommerceX: Shipment update — status: SHIPPED. Track: a1b2c3d4-.... ETA: 2026-04-04
[SMS] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Error Handling
Notification failures are caught and logged — they **never** propagate to the caller or roll back transactions. This ensures a failed notification doesn't block order/shipment processing.

```java
try {
    emailNotification.sendOrderConfirmation(user, order);
    smsNotification.sendOrderConfirmation(user, order);
} catch (Exception e) {
    log.error("Failed to send notifications for order {}: {}", orderId, e.getMessage());
}
```

---

## Future Extensions
- Replace `EmailNotificationServiceImpl` with real SMTP (e.g., Spring Mail + SendGrid)
- Replace `SmsNotificationServiceImpl` with real SMS (e.g., Twilio)
- Add `phone` field to `User` entity for real SMS delivery
- Add notification preferences (opt-in/opt-out per channel)
