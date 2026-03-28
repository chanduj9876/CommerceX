# CommerceX API Reference

Base URL: `http://localhost:8080`

---

## API Documentation

| Module | File | Endpoints |
|--------|------|-----------|
| Auth | [AUTH_API.md](AUTH_API.md) | Register, Login, Change/Reset Password |
| Category | [CATEGORY_API.md](CATEGORY_API.md) | CRUD + Products by Category |
| Product | [PRODUCT_API.md](PRODUCT_API.md) | CRUD + Search/Filter |
| Cart | [CART_API.md](CART_API.md) | View, Add, Update, Remove, Clear |
| Order | [ORDER_API.md](ORDER_API.md) | Checkout, My Orders, Status Lifecycle, Cancel |
| Payment | [PAYMENT_API.md](PAYMENT_API.md) | Initiate, View by Order, View by Transaction |
| Shipment | [SHIPMENT_API.md](SHIPMENT_API.md) | Create, Track, Status Update, Order Lookup |
| Notification | [NOTIFICATION_API.md](NOTIFICATION_API.md) | Simulated Email + SMS (event-driven) |

---

## Access Rules

| Endpoint | Access |
|----------|--------|
| `POST /api/v1/auth/register` | Public |
| `POST /api/v1/auth/login` | Public |
| `POST /api/v1/auth/forgot-password` | Public |
| `POST /api/v1/auth/reset-password` | Public |
| `PUT /api/v1/auth/change-password` | Authenticated |
| `GET /api/v1/products/**` | Public |
| `GET /api/v1/categories/**` | Public |
| POST/PUT/PATCH/DELETE products | ADMIN only |
| POST/PUT/PATCH/DELETE categories | ADMIN only |
| `GET/POST/PUT/DELETE /api/v1/cart/**` | Authenticated |
| `POST /api/v1/orders` | Authenticated |
| `GET /api/v1/orders/my-orders` | Authenticated |
| `GET /api/v1/orders/{id}` | Authenticated |
| `PATCH /api/v1/orders/{id}/cancel` | Authenticated (owner) |
| `GET /api/v1/orders` | ADMIN only |
| `PATCH /api/v1/orders/{id}/status` | ADMIN only |
| `POST /api/v1/payments/initiate` | Authenticated (order owner) |
| `GET /api/v1/payments/order/{orderId}` | Authenticated (order owner) |
| `GET /api/v1/payments/{transactionId}` | Authenticated |
| `POST /api/v1/shipments` | ADMIN only |
| `GET /api/v1/shipments/{trackingId}` | Authenticated |
| `PATCH /api/v1/shipments/{trackingId}/status` | ADMIN only |
| `GET /api/v1/shipments/order/{orderId}` | Authenticated |

---

## Error Responses

All errors return a consistent JSON format:

```json
{
  "status": 404,
  "message": "Category not found with id: 1",
  "timestamp": "2026-03-24T10:30:00"
}
```

| Status | Scenario |
|--------|----------|
| `400 Bad Request` | Validation failed, empty cart, insufficient stock, invalid promo code, invalid order state |
| `401 Unauthorized` | Invalid email or password / missing token |
| `403 Forbidden` | Valid token but insufficient role (e.g., CUSTOMER on admin endpoint) |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Duplicate name or email / concurrent stock update |
| `422 Unprocessable Entity` | Invalid state transition (e.g., non-CONFIRMED order, invalid shipment status) |

---

## Database Schema

> PostgreSQL · `jdbc:postgresql://localhost:5432/commercex` · DDL: `spring.jpa.hibernate.ddl-auto=update`

### Full ER Diagram

```mermaid
erDiagram
    users {
        bigint id PK
        varchar name
        varchar email UK
        varchar password
        varchar role "CUSTOMER | ADMIN"
        timestamp created_at
        timestamp updated_at
    }

    password_reset_tokens {
        bigint id PK
        varchar token UK
        bigint user_id FK
        timestamp expires_at
    }

    categories {
        bigint id PK
        varchar name UK
        text description
        timestamp created_at
        timestamp updated_at
    }

    products {
        bigint id PK
        varchar name UK
        text description
        numeric price "POSITIVE"
        integer stock ">=0"
        bigint version "optimistic lock"
        timestamp created_at
        timestamp updated_at
    }

    product_categories {
        bigint product_id FK
        bigint category_id FK
    }

    carts {
        bigint id PK
        bigint user_id FK "UNIQUE"
        timestamp created_at
        timestamp updated_at
    }

    cart_items {
        bigint id PK
        bigint cart_id FK
        bigint product_id FK
        integer quantity "1-999"
        timestamp created_at
        timestamp updated_at
    }

    orders {
        bigint id PK
        bigint user_id FK
        numeric total_amount
        numeric discount_amount
        varchar promo_code
        varchar status "PENDING | CONFIRMED | SHIPPED | DELIVERED | CANCELLED"
        timestamp created_at
        timestamp updated_at
    }

    order_items {
        bigint id PK
        bigint order_id FK
        bigint product_id "snapshot"
        varchar product_name "snapshot"
        numeric unit_price "snapshot"
        integer quantity
        timestamp created_at
        timestamp updated_at
    }

    payments {
        bigint id PK
        bigint order_id FK
        decimal amount "12,2"
        varchar status "PENDING | SUCCESS | FAILED | REFUNDED"
        varchar method "CREDIT_CARD | UPI | WALLET"
        varchar transaction_id UK "UUID"
        varchar reference_id "gateway ref"
        varchar failure_reason
        timestamp created_at
        timestamp updated_at
    }

    shipments {
        bigint id PK
        bigint order_id FK
        varchar tracking_id UK "UUID"
        varchar status "PROCESSING | SHIPPED | IN_TRANSIT | DELIVERED"
        date estimated_delivery
        timestamp created_at
        timestamp updated_at
    }

    users ||--o{ password_reset_tokens : "has reset tokens"
    users ||--|| carts : "has one cart"
    users ||--o{ orders : "places"
    carts ||--o{ cart_items : "contains"
    products ||--o{ cart_items : "in cart"
    products ||--o{ product_categories : "belongs to"
    categories ||--o{ product_categories : "contains"
    orders ||--o{ order_items : "contains"
    orders ||--o{ payments : "has"
    orders ||--o| shipments : "has"
```

### Table Summary

| Table | Rows Represent | Key Constraints |
|-------|---------------|-----------------|
| `users` | Registered accounts | email UNIQUE |
| `password_reset_tokens` | Temporary reset tokens | token UNIQUE, FK → users |
| `categories` | Product categories | name UNIQUE |
| `products` | Catalog items | name UNIQUE, `@Version` for optimistic locking |
| `product_categories` | Product ↔ Category mapping | Composite PK (product_id, category_id) |
| `carts` | One shopping cart per user | user_id UNIQUE (1:1) |
| `cart_items` | Items in a cart | UNIQUE (cart_id, product_id), FK → products |
| `orders` | Completed checkouts | FK → users, status as STRING enum |
| `order_items` | Snapshot of purchased items | FK → orders, product data is **snapshotted** (not FK) |
| `payments` | Payment attempts | FK → orders, transaction_id UNIQUE, indexed by order_id |
| `shipments` | Shipment tracking | FK → orders, tracking_id UNIQUE (UUID) |

### Design Decisions

| Decision | Rationale |
|----------|-----------|
| **BIGSERIAL PKs** | Auto-increment, database-managed identity generation |
| **BigDecimal for money** | Avoids floating-point precision errors (0.1 + 0.2 ≠ 0.3 with double) |
| **@Version on products** | Optimistic locking prevents concurrent stock overwrites (409 on conflict) |
| **Enum as STRING** | Stored as text (CUSTOMER, ADMIN) — safe to reorder enum values |
| **Snapshot in order_items** | Product name/price locked at checkout — survives price changes and deletion |
| **Cascade + orphanRemoval** | Cart items and order items auto-delete when removed from parent |
| **Unique (cart_id, product_id)** | One row per product in a cart — duplicates merge quantities |
| **No FK on order_items.product_id** | Intentional — order history must survive product deletion |
| **ManyToOne on payments→orders** | One order can have multiple payment attempts (retry after failure) |
| **Event-driven order confirmation** | Payment success publishes RabbitMQ event → order module reacts independently |
| **Event-driven shipments** | Order CONFIRMED → auto-creates shipment via Spring event listener |
| **Simulated notifications** | Email + SMS logged via SLF4J — swap with real providers later |

---

## Complete Entity Relationship Diagram

> All entities extend `BaseEntity` (id, createdAt, updatedAt).

```mermaid
classDiagram
    direction TB

    class BaseEntity {
        <<abstract>>
        #Long id
        #LocalDateTime createdAt
        #LocalDateTime updatedAt
        #onCreate() void
        #onUpdate() void
    }

    class User {
        -String name
        -String email
        -String password
        -Role role
    }

    class Role {
        <<enumeration>>
        CUSTOMER
        ADMIN
    }

    class PasswordResetToken {
        -Long id
        -String token
        -User user
        -LocalDateTime expiresAt
        +isExpired() boolean
    }

    class Category {
        -String name
        -String description
        -Set~Product~ products
    }

    class Product {
        -String name
        -String description
        -BigDecimal price
        -Integer stock
        -Long version
        -Set~Category~ categories
    }

    class Cart {
        -User user
        -List~CartItem~ items
        +addItem(CartItem) void
        +removeItem(Long) void
        +clear() void
    }

    class CartItem {
        -Cart cart
        -Product product
        -Integer quantity
    }

    class Order {
        -User user
        -List~OrderItem~ items
        -BigDecimal totalAmount
        -BigDecimal discountAmount
        -String promoCode
        -OrderStatus status
        +addItem(OrderItem) void
    }

    class OrderItem {
        -Order order
        -Long productId
        -String productName
        -BigDecimal unitPrice
        -Integer quantity
        +getSubtotal() BigDecimal
    }

    class OrderStatus {
        <<enumeration>>
        PENDING
        CONFIRMED
        SHIPPED
        DELIVERED
        CANCELLED
    }

    BaseEntity <|-- User
    BaseEntity <|-- Category
    BaseEntity <|-- Product
    BaseEntity <|-- Cart
    BaseEntity <|-- CartItem
    BaseEntity <|-- Order
    BaseEntity <|-- OrderItem
    BaseEntity <|-- Payment

    User --> Role
    PasswordResetToken "*" --> "1" User : ManyToOne
    Product "*" -- "*" Category : ManyToMany
    Cart "1" --> "1" User : OneToOne
    Cart "1" --> "*" CartItem : OneToMany
    CartItem "*" --> "1" Product : ManyToOne
    Order "*" --> "1" User : ManyToOne
    Order "1" --> "*" OrderItem : OneToMany
    Order --> OrderStatus
    Payment "*" --> "1" Order : ManyToOne
    Payment --> PaymentStatus
    Payment --> PaymentMethod
```

---

## Service Layer Architecture

```mermaid
classDiagram
    direction LR

    class AuthController {
        +register()
        +login()
        +changePassword()
        +forgotPassword()
        +resetPassword()
    }

    class CategoryController {
        +getAllCategories()
        +getCategoryById()
        +createCategory()
        +updateCategory()
        +deleteCategory()
    }

    class ProductController {
        +getAllProducts()
        +getProductById()
        +searchProducts()
        +createProduct()
        +updateProduct()
        +deleteProduct()
    }

    class CartController {
        +getCart()
        +addItem()
        +updateItemQuantity()
        +removeItem()
        +clearCart()
    }

    class OrderController {
        +createOrder()
        +getMyOrders()
        +getOrderById()
        +cancelOrder()
        +getAllOrders()
        +updateOrderStatus()
    }

    class PaymentController {
        +initiatePayment()
        +getPaymentsByOrder()
        +getPaymentByTransactionId()
    }

    class AuthService {
        <<interface>>
    }
    class CategoryService {
        <<interface>>
    }
    class ProductService {
        <<interface>>
    }
    class CartService {
        <<interface>>
    }
    class OrderService {
        <<interface>>
    }
    class PaymentService {
        <<interface>>
    }

    class AuthServiceImpl
    class CategoryServiceImpl
    class ProductServiceImpl
    class CartServiceImpl
    class OrderServiceImpl
    class PaymentServiceImpl

    class UserRepository {
        <<interface>>
    }
    class CategoryRepository {
        <<interface>>
    }
    class ProductRepository {
        <<interface>>
    }
    class CartRepository {
        <<interface>>
    }
    class OrderRepository {
        <<interface>>
    }
    class PaymentRepository {
        <<interface>>
    }

    AuthController --> AuthService
    CategoryController --> CategoryService
    ProductController --> ProductService
    CartController --> CartService
    OrderController --> OrderService
    PaymentController --> PaymentService

    AuthService <|.. AuthServiceImpl
    CategoryService <|.. CategoryServiceImpl
    ProductService <|.. ProductServiceImpl
    CartService <|.. CartServiceImpl
    OrderService <|.. OrderServiceImpl
    PaymentService <|.. PaymentServiceImpl

    AuthServiceImpl --> UserRepository
    CategoryServiceImpl --> CategoryRepository
    ProductServiceImpl --> ProductRepository
    ProductServiceImpl --> CategoryService : resolves categories
    CartServiceImpl --> CartRepository
    CartServiceImpl --> ProductRepository : validates stock
    OrderServiceImpl --> OrderRepository
    OrderServiceImpl --> CartRepository : reads cart
    OrderServiceImpl --> ProductRepository : deducts stock
    OrderServiceImpl --> DiscountStrategyFactory
    OrderServiceImpl --> OrderEventPublisher

    PaymentServiceImpl --> PaymentRepository
    PaymentServiceImpl --> OrderRepository : validates order
    PaymentServiceImpl --> PaymentGatewayFactory
    PaymentServiceImpl --> PaymentEventProducer

    class DiscountStrategyFactory
    class OrderEventPublisher
    class PaymentGatewayFactory
    class PaymentEventProducer
```

---

## Security Architecture

```mermaid
classDiagram
    direction TB

    class SecurityConfig {
        -JwtAuthenticationFilter jwtFilter
        +securityFilterChain() SecurityFilterChain
        +passwordEncoder() PasswordEncoder
        +authenticationManager() AuthenticationManager
    }

    class JwtAuthenticationFilter {
        -JwtUtil jwtUtil
        +doFilterInternal() void
    }

    class JwtUtil {
        -SecretKey secretKey
        -long expirationMs
        +generateToken(UserDetails) String
        +extractUsername(String) String
        +extractRoles(String) String
        +validateToken(String, String) boolean
    }

    class UserDetailsServiceImpl {
        -UserRepository userRepository
        +loadUserByUsername(String) UserDetails
    }

    class StrongPassword {
        <<annotation>>
        min 8 chars, upper, lower, digit, special
    }

    class StrongPasswordValidator {
        +isValid(String, Context) boolean
    }

    SecurityConfig --> JwtAuthenticationFilter
    JwtAuthenticationFilter --> JwtUtil
    UserDetailsServiceImpl --> UserRepository
    StrongPassword --> StrongPasswordValidator
```

---

## Discount Strategy Pattern

```mermaid
classDiagram
    direction TB

    class DiscountStrategy {
        <<interface>>
        +calculateDiscount(BigDecimal total) BigDecimal
    }

    class NoDiscount {
        +calculateDiscount(BigDecimal) BigDecimal
    }

    class PercentageDiscount {
        -BigDecimal rate
        +calculateDiscount(BigDecimal) BigDecimal
    }

    class FlatDiscount {
        -BigDecimal amount
        +calculateDiscount(BigDecimal) BigDecimal
    }

    class DiscountStrategyFactory {
        +getStrategy(String promoCode) DiscountStrategy
        +isValidPromoCode(String) boolean
    }

    DiscountStrategy <|.. NoDiscount
    DiscountStrategy <|.. PercentageDiscount
    DiscountStrategy <|.. FlatDiscount
    DiscountStrategyFactory --> DiscountStrategy : creates

    note for DiscountStrategyFactory "Promo Codes:\nSAVE10 → 10%\nSAVE20 → 20%\nFLAT50 → $50\nWELCOME → 15%"
```

---

## Payment Adapter Pattern

```mermaid
classDiagram
    direction TB

    class PaymentGateway {
        <<interface>>
        +processPayment(BigDecimal, String) GatewayResponse
    }

    class GatewayResponse {
        -boolean success
        -String referenceId
        -String failureReason
        +success(String) GatewayResponse$
        +failure(String) GatewayResponse$
    }

    class CreditCardGatewayAdapter {
        +processPayment() GatewayResponse
    }

    class UpiGatewayAdapter {
        +processPayment() GatewayResponse
    }

    class WalletGatewayAdapter {
        +processPayment() GatewayResponse
    }

    class PaymentGatewayFactory {
        +getGateway(PaymentMethod) PaymentGateway
    }

    class PaymentEventProducer {
        +publishPaymentCompleted(Long, String) void
    }

    class PaymentEventConsumer {
        +handlePaymentCompleted(PaymentCompletedEvent) void
    }

    PaymentGateway <|.. CreditCardGatewayAdapter
    PaymentGateway <|.. UpiGatewayAdapter
    PaymentGateway <|.. WalletGatewayAdapter
    PaymentGatewayFactory --> PaymentGateway : creates
    PaymentGateway --> GatewayResponse : returns
    PaymentEventProducer ..> PaymentEventConsumer : "RabbitMQ\npayment.completed"

    note for CreditCardGatewayAdapter "~80% success rate"
    note for UpiGatewayAdapter "~90% success rate"
    note for WalletGatewayAdapter "~95% success rate"
```

---

## Exception Hierarchy

```mermaid
classDiagram
    direction TB

    class RuntimeException

    class BusinessException {
        <<abstract>>
        +getStatus() HttpStatus
    }

    class ResourceNotFoundException {
        +getStatus() 404
    }
    class DuplicateResourceException {
        +getStatus() 409
    }
    class DuplicateEmailException {
        +getStatus() 409
    }
    class EmptyCartException {
        +getStatus() 400
    }
    class InsufficientStockException {
        +getStatus() 400
    }
    class InvalidOrderStateException {
        +getStatus() 400
    }
    class InvalidPasswordException {
        +getStatus() 400
    }
    class InvalidPromoCodeException {
        +getStatus() 400
    }

    class GlobalExceptionHandler {
        +handleBusinessException() ResponseEntity
        +handleValidationErrors() ResponseEntity
        +handleBadCredentials() ResponseEntity
        +handleAccessDenied() ResponseEntity
        +handleOptimisticLock() ResponseEntity
    }

    class ErrorResponse {
        -int status
        -String message
        -LocalDateTime timestamp
    }

    RuntimeException <|-- BusinessException
    BusinessException <|-- ResourceNotFoundException
    BusinessException <|-- DuplicateResourceException
    BusinessException <|-- DuplicateEmailException
    BusinessException <|-- EmptyCartException
    BusinessException <|-- InsufficientStockException
    BusinessException <|-- InvalidOrderStateException
    BusinessException <|-- InvalidPasswordException
    BusinessException <|-- InvalidPromoCodeException
    GlobalExceptionHandler ..> BusinessException : catches
    GlobalExceptionHandler ..> ErrorResponse : returns
```

---

## Order Status State Machine

```mermaid
stateDiagram-v2
    [*] --> PENDING : Order Created
    PENDING --> CONFIRMED : Payment succeeds (via RabbitMQ)
    PENDING --> CANCELLED : Customer/Admin cancels
    CONFIRMED --> SHIPPED : Admin ships
    CONFIRMED --> CANCELLED : Admin cancels
    SHIPPED --> DELIVERED : Admin delivers
    DELIVERED --> [*] : Terminal
    CANCELLED --> [*] : Terminal (stock restored)
```

---

## Individual Module Diagrams

Each API reference file contains its own focused class diagram:

| Module | Diagram Location |
|--------|-----------------|
| Auth & Security | [AUTH_API.md](AUTH_API.md#class-diagram) |
| Category | [CATEGORY_API.md](CATEGORY_API.md#class-diagram) |
| Product | [PRODUCT_API.md](PRODUCT_API.md#class-diagram) |
| Cart | [CART_API.md](CART_API.md#class-diagram) |
| Order & Discount | [ORDER_API.md](ORDER_API.md#class-diagram) |
| Payment & Gateway | [PAYMENT_API.md](PAYMENT_API.md#class-diagram) |
