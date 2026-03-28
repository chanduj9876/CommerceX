# Shipment API Reference

## Overview
The Shipment module provides shipment tracking for confirmed orders. Shipments are auto-created when an order transitions to `CONFIRMED` (via payment success), and admins can update the shipment status through its lifecycle.

**Base URL:** `/api/v1/shipments`  
**Authentication:** All endpoints require a valid JWT token (Bearer).

---

## Endpoints

### 1. Create Shipment
**POST** `/api/v1/shipments` 🔒 ADMIN only

Manually creates a shipment for a confirmed order. Normally shipments are auto-created when an order is confirmed, but this provides a manual override.

**Request Body:**
```json
{
  "orderId": 1
}
```

| Field   | Type | Required | Description                            |
|---------|------|----------|----------------------------------------|
| orderId | Long | Yes      | The order to create a shipment for (must be CONFIRMED) |

**Success Response (201 Created):**
```json
{
  "id": 1,
  "orderId": 1,
  "trackingId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "PROCESSING",
  "estimatedDelivery": "2026-04-04",
  "createdAt": "2026-03-28T14:00:00",
  "updatedAt": "2026-03-28T14:00:00"
}
```

**Error Responses:**
| Status | Condition                                     |
|--------|-----------------------------------------------|
| 404    | Order not found                               |
| 422    | Order is not in CONFIRMED status              |
| 422    | Shipment already exists for this order        |

---

### 2. Track Shipment
**GET** `/api/v1/shipments/{trackingId}`

Returns the current status and details of a shipment by its UUID tracking ID.

```bash
curl http://localhost:8080/api/v1/shipments/a1b2c3d4-e5f6-7890-abcd-ef1234567890 \
  -H "Authorization: Bearer <token>"
```

**Success Response (200 OK):**
```json
{
  "id": 1,
  "orderId": 1,
  "trackingId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "SHIPPED",
  "estimatedDelivery": "2026-04-04",
  "createdAt": "2026-03-28T14:00:00",
  "updatedAt": "2026-03-28T15:30:00"
}
```

**Error Responses:**
| Status | Condition                                |
|--------|------------------------------------------|
| 404    | Shipment not found with that tracking ID |

---

### 3. Update Shipment Status
**PATCH** `/api/v1/shipments/{trackingId}/status` 🔒 ADMIN only

Updates the shipment status. Transitions are validated — see the status lifecycle below.

**Request Body:**
```json
{
  "status": "SHIPPED"
}
```

| Field  | Type   | Required | Description                                      |
|--------|--------|----------|--------------------------------------------------|
| status | String | Yes      | `PROCESSING`, `SHIPPED`, `IN_TRANSIT`, `DELIVERED` |

**Success Response (200 OK):**
```json
{
  "id": 1,
  "orderId": 1,
  "trackingId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "SHIPPED",
  "estimatedDelivery": "2026-04-04",
  "createdAt": "2026-03-28T14:00:00",
  "updatedAt": "2026-03-28T16:00:00"
}
```

**Error Responses:**
| Status | Condition                                                       |
|--------|-----------------------------------------------------------------|
| 404    | Shipment not found with that tracking ID                        |
| 422    | Invalid status transition (e.g., PROCESSING → DELIVERED)        |

---

### 4. Get Shipment by Order
**GET** `/api/v1/shipments/order/{orderId}`

Returns the shipment associated with a specific order.

```bash
curl http://localhost:8080/api/v1/shipments/order/1 \
  -H "Authorization: Bearer <token>"
```

**Success Response (200 OK):**
```json
{
  "id": 1,
  "orderId": 1,
  "trackingId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "IN_TRANSIT",
  "estimatedDelivery": "2026-04-04",
  "createdAt": "2026-03-28T14:00:00",
  "updatedAt": "2026-03-29T10:00:00"
}
```

**Error Responses:**
| Status | Condition                            |
|--------|--------------------------------------|
| 404    | No shipment found for that order     |

---

## Shipment Status Lifecycle

```
PROCESSING → SHIPPED → IN_TRANSIT → DELIVERED
```

| Status     | Description                           | Valid Next Status |
|------------|---------------------------------------|-------------------|
| PROCESSING | Shipment created, being prepared      | SHIPPED           |
| SHIPPED    | Handed off to carrier                 | IN_TRANSIT        |
| IN_TRANSIT | On the way to the customer            | DELIVERED         |
| DELIVERED  | Customer received the package (final) | —                 |

> `DELIVERED` is a terminal state — no further transitions allowed.

---

## Event-Driven Behavior

### Auto-Shipment Creation
When an order transitions to `CONFIRMED` (via payment success), the `OrderEventListener` automatically creates a shipment:

```
Payment SUCCESS → Order CONFIRMED → Shipment auto-created (PROCESSING)
```

### Shipment Notifications
Every shipment status update triggers email and SMS notifications (simulated via log output):

```
Shipment status change → ShipmentStatusChangedEvent → ShipmentEventListener
                                                        ├─ EmailNotificationService (logs email)
                                                        └─ SmsNotificationService (logs SMS)
```

---

## Full Order → Shipment Flow

```
Customer                    PaymentService         OrderEventListener      ShipmentService          ShipmentEventListener
   │                              │                       │                       │                        │
   │  Payment SUCCESS             │                       │                       │                        │
   │─────────────────────────────>│                       │                       │                        │
   │                              │  Order → CONFIRMED    │                       │                        │
   │                              │──────────────────────>│                       │                        │
   │                              │                       │  createShipment()     │                        │
   │                              │                       │──────────────────────>│                        │
   │                              │                       │                       │  Save PROCESSING       │
   │                              │                       │                       │                        │
   │                              │                       │  Send notifications   │                        │
   │                              │                       │  (email + SMS)        │                        │
   │                              │                       │                       │                        │
   │                              │                       │                       │                        │
Admin                                                                             │                        │
   │  PATCH /shipments/{id}/status (SHIPPED)              │                       │                        │
   │─────────────────────────────────────────────────────────────────────────────>│                        │
   │                                                                              │  Publish event         │
   │                                                                              │───────────────────────>│
   │                                                                              │                        │  Send notifications
   │                                                                              │                        │  (email + SMS)
   │  ShipmentResponseDTO                                                         │                        │
   │<─────────────────────────────────────────────────────────────────────────────│                        │
```
