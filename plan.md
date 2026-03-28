# Plan: CommerceX — 100-Task E-Commerce Backend Build

## TL;DR
Build a full production-grade e-commerce backend in 8 phases on top of the existing Spring Boot 4.0.4 / Java 21 scaffold. Tasks are ordered easy → hard within each phase, covering entity design, REST APIs, security, cart/orders, payments, inventory, shipping, microservices split, and deployment.

---

## Phase 1 — Core Setup & Product Catalog (Tasks 1–15)
**Goal:** Working product management REST API.

1. Add `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql` driver, `spring-boot-starter-validation`, and `lombok` dependencies to `pom.xml`.
2. Configure `application.properties` with datasource URL, username, password, `spring.jpa.hibernate.ddl-auto=update`, and `spring.jpa.show-sql=true`.
3. Create modular package structure: `product`, `user`, `order`, `cart`, `payment`, `shipping`, `common` under `com.commercex`.
4. Create `Product` entity (`id`, `name`, `description`, `price`, `stock`, `createdAt`, `updatedAt`) with `@Entity`, `@Table`, JPA annotations, `@PrePersist`/`@PreUpdate` lifecycle hooks.
5. Add Bean Validation constraints to `Product`: `@NotBlank(name)`, `@Positive(price)`, `@PositiveOrZero(stock)`, `@Column(unique=true)` on name.
6. Create `ProductRepository` extending `JpaRepository<Product, Long>` with a `findByName` method.
7. Create `ProductRequestDTO` (name, description, price, stock) with `@Valid` annotations.
8. Create `ProductResponseDTO` (all fields including timestamps).
9. Create `ProductMapper` (static/MapStruct) converting between `Product` ↔ `ProductRequestDTO` ↔ `ProductResponseDTO`.
10. Create `ProductService` interface and `ProductServiceImpl` with `createProduct`, `getProductById`, `getAllProducts`, `updateProduct`, `deleteProduct`.
11. Create `ProductController` with `GET /api/v1/products` and `GET /api/v1/products/{id}`.
12. Add `POST /api/v1/products` to `ProductController` using `@RequestBody @Valid ProductRequestDTO`.
13. Add `PUT /api/v1/products/{id}` and `DELETE /api/v1/products/{id}` to `ProductController`.
14. Create `ResourceNotFoundException` and `GlobalExceptionHandler` (`@RestControllerAdvice`) returning a structured `ErrorResponse` DTO.
15. Write unit tests for `ProductServiceImpl` (MockMvc or Mockito) covering create, get, update, delete, and not-found cases.

---

## Phase 2 — User & Authentication (Tasks 16–30)
**Goal:** JWT-secured login/registration with role-based access.

16. Add `spring-boot-starter-security`, `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` dependencies to `pom.xml`.
17. Create `Role` enum: `CUSTOMER`, `ADMIN`.
18. Create `User` entity (`id`, `name`, `email` UNIQUE, `password`, `role`, `createdAt`) with proper JPA annotations.
19. Create `UserRepository` with `findByEmail(String email)` method.
20. Create `RegisterRequest` DTO (name, email, password) and `LoginRequest` DTO (email, password) with `@Valid` annotations.
21. Create `AuthResponse` DTO (token, email, role).
22. Create `UserDetailsServiceImpl` implementing Spring Security's `UserDetailsService`, loading `User` by email.
23. Create `JwtUtil` (Singleton pattern) — `generateToken(UserDetails)`, `validateToken(String, UserDetails)`, `extractUsername(String)` using HS256 and configurable secret/expiry from `application.properties`.
24. Create `JwtAuthenticationFilter` extending `OncePerRequestFilter` — extracts Bearer token, validates it, sets `SecurityContextHolder`.
25. Create `SecurityConfig` (`@Configuration`, `@EnableWebSecurity`) — stateless session, CSRF disabled, permit `/auth/**`, require auth for all others, wire `JwtAuthenticationFilter`.
26. Implement `UserService` with `register` (check duplicate email, BCrypt hash, save) and `login` (authenticate, return JWT) logic.
27. Create `AuthController` with `POST /api/v1/auth/register` and `POST /api/v1/auth/login`.
28. Enable `@EnableMethodSecurity` and annotate `ProductController` write endpoints with `@PreAuthorize("hasRole('ADMIN')")`.
29. Add `DuplicateEmailException` to `GlobalExceptionHandler` (HTTP 409).
30. Write unit tests for `JwtUtil` (token generation/validation) and `AuthService` (register + login flows).

---

## Phase 3 — Shopping Cart & Orders (Tasks 31–48)
**Goal:** Customers can build a cart and place orders.

31. Create `CartItem` entity (`id`, `product`, `quantity`, `cart`) with `@ManyToOne` to `Product`.
32. Create `Cart` entity (`id`, `user` OneToOne, `items` OneToMany to `CartItem`) with cascade operations.
33. Create `CartRepository` with `findByUserId` and `CartItemRepository`.
34. Create `CartService` — `addItem(userId, productId, qty)`, `removeItem(userId, cartItemId)`, `clearCart(userId)`, `getCart(userId)`.
35. Create `CartController` (`GET`, `POST`, `DELETE` under `/api/v1/cart`), secured for `CUSTOMER` and `ADMIN`.
36. Create `OrderStatus` enum: `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`.
37. Create `OrderItem` entity (`id`, `order`, `productId`, `productName`, `unitPrice`, `quantity`) — snapshot product data at order time.
38. Create `Order` entity (`id`, `user`, `items` OneToMany, `totalAmount`, `status`, `createdAt`, `updatedAt`).
39. Create `OrderRepository` with `findByUserId` and `OrderItemRepository`.
40. Create `DiscountStrategy` interface (`applyDiscount(BigDecimal total): BigDecimal`) — Strategy pattern.
41. Implement `NoDiscount`, `PercentageDiscount(rate)`, `FlatDiscount(amount)` strategies.
42. Create `DiscountStrategyFactory` returning the correct strategy based on user role or promo code.
43. Create `OrderService` — `createOrder(userId)` converts cart → order (applies discount, snapshots product data, clears cart) wrapped in `@Transactional`.
44. Add `getOrderById(orderId)`, `getOrdersByUser(userId)`, `updateOrderStatus(orderId, status)` to `OrderService`.
45. Create `OrderController` (`POST /api/v1/orders`, `GET /api/v1/orders/{id}`, `GET /api/v1/orders/my-orders`, `PATCH /api/v1/orders/{id}/status`).
46. Create `OrderEventPublisher` (Observer pattern) — Spring `ApplicationEventPublisher` publishing `OrderStatusChangedEvent(orderId, newStatus)`.
47. Create `OrderEventListener` (`@EventListener`) logging status changes (to be wired with notifications in Phase 6).
48. Write unit tests for `OrderService.createOrder` including discount logic and `@Transactional` rollback scenario.

---

## Phase 4 — Payment Integration Simulation (Tasks 49–60)
**Goal:** Simulated payment gateway with event-driven status updates.

49. Add `spring-boot-starter-amqp` (RabbitMQ) or `spring-kafka` dependency; choose RabbitMQ for simplicity.
50. Create `PaymentStatus` enum: `PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`.
51. Create `PaymentMethod` enum: `CREDIT_CARD`, `UPI`, `WALLET`.
52. Create `Payment` entity (`id`, `orderId`, `amount`, `status`, `method`, `transactionId`, `createdAt`).
53. Create `PaymentRepository` with `findByOrderId`.
54. Create `PaymentGateway` interface (`processPayment(PaymentRequestDTO): PaymentResponseDTO`) — Adapter pattern.
55. Implement `CreditCardGatewayAdapter`, `UpiGatewayAdapter`, `WalletGatewayAdapter` (simulated — random success/fail with log output).
56. Create `PaymentGatewayFactory` (Factory pattern) returning the correct adapter based on `PaymentMethod`.
57. Create `PaymentService` — `initiatePayment(orderId, method, amount)` saves PENDING payment and routes to gateway; `confirmPayment(transactionId)` updates status.
58. Create `PaymentController` (`POST /api/v1/payments/initiate`, `POST /api/v1/payments/confirm`, `GET /api/v1/payments/{orderId}`).
59. Configure RabbitMQ exchange/queue (`RabbitMQConfig`) and create `PaymentEventProducer` publishing `PaymentCompletedEvent` on success.
60. Create `PaymentEventConsumer` (`@RabbitListener`) updating `Order` status to `CONFIRMED` on payment success.

---

## Phase 5 — Inventory & Stock Management (Tasks 61–70)
**Goal:** Accurate, concurrency-safe stock tracking with caching.

61. Add `@Version Long version` field to `Product` entity for optimistic locking.
62. In `OrderService.createOrder`, deduct stock per `OrderItem` quantity before saving the order (within the same `@Transactional`).
63. Throw `InsufficientStockException` if `product.getStock() < requestedQty`; add to `GlobalExceptionHandler` (HTTP 422).
64. Handle `ObjectOptimisticLockingFailureException` in `GlobalExceptionHandler` (HTTP 409 — retry suggested).
65. Create `StockUpdateRequest` DTO (productId, quantity, operation: ADD / SUBTRACT).
66. Add `PATCH /api/v1/products/{id}/stock` admin endpoint to `ProductController` calling `ProductService.updateStock`.
67. Add `spring-boot-starter-data-redis` and `spring-boot-starter-cache` dependencies to `pom.xml`.
68. Configure Redis in `application.properties` (host, port, TTL) and create `CacheConfig` with `@EnableCaching` and `RedisCacheManager`.
69. Apply `@Cacheable("products")` to `ProductService.getProductById` and `getAllProducts`; `@CacheEvict` on update, delete, and stock change.
70. Write integration tests verifying stock deduction and cache invalidation (use `@SpringBootTest` with H2 / Testcontainers).

---

## Phase 6 — Shipping & Notifications (Tasks 71–80)
**Goal:** Shipment tracking and simulated customer notifications.

71. Create `ShipmentStatus` enum: `PROCESSING`, `SHIPPED`, `IN_TRANSIT`, `DELIVERED`.
72. Create `Shipment` entity (`id`, `orderId`, `trackingId` UUID, `status`, `estimatedDelivery`).
73. Create `ShipmentRepository` with `findByOrderId` and `findByTrackingId`.
74. Create `ShipmentService` — `createShipment(orderId)` (generates UUID tracking id), `updateStatus(trackingId, status)`, `trackShipment(trackingId)`.
75. Create `ShipmentController` (`POST /api/v1/shipments`, `GET /api/v1/shipments/{trackingId}`, `PATCH /api/v1/shipments/{trackingId}/status`).
76. In `OrderEventListener`, auto-invoke `ShipmentService.createShipment` when `OrderStatus` transitions to `CONFIRMED`.
77. Create `NotificationService` interface with `sendOrderConfirmation(User, Order)`, `sendShipmentUpdate(User, Shipment)`.
78. Implement `EmailNotificationServiceImpl` (simulated — format and log email content to console/Slf4j).
79. Implement `SmsNotificationServiceImpl` (simulated — log SMS content).
80. Wire both notification services into `OrderEventListener` and a new `ShipmentEventListener`.

---

## Phase 7 — API Gateway & Microservices (Tasks 81–90)
**Goal:** Modular microservices with centralized routing and service discovery.

81. Add `spring-cloud-dependencies` BOM (matching Spring Boot 4.x compatible version) to `pom.xml` and convert project to Maven multi-module parent POM.
82. Create `product-service` Maven module: migrate `product` package + its dependencies/config.
83. Create `user-service` Maven module: migrate `user`, `auth`, JWT config.
84. Create `order-service` Maven module: migrate `cart`, `order`, `discount` packages.
85. Create `payment-service` Maven module: migrate `payment` package and RabbitMQ config.
86. Create `shipping-service` Maven module: migrate `shipment`, `notification` packages.
87. Add `eureka-server` Maven module: `spring-cloud-starter-netflix-eureka-server`, configure `application.yml`, annotate main class `@EnableEurekaServer`.
88. Register each service as Eureka client (`@EnableEurekaClient`, `eureka.client.service-url`).
89. Create `api-gateway` Maven module with `spring-cloud-starter-gateway`; define route predicates for all five services in `application.yml`; forward JWT header for auth.
90. Configure inter-service HTTP calls with `WebClient` or `FeignClient` (e.g., `order-service` calls `product-service` for stock check) using Eureka load balancing.

---

## Phase 8 — Documentation & Deployment (Tasks 91–100)
**Goal:** Production-ready, documented, containerized system.

91. Add `springdoc-openapi-starter-webmvc-ui` (compatible with Spring Boot 4.x) to each service's `pom.xml`.
92. Configure `OpenAPI` bean in each service with `Info` (title, version, description) and `SecurityScheme` for Bearer JWT.
93. Annotate all Controllers with `@Tag` and `@Operation(summary, description)`.
94. Annotate all request/response DTOs with `@Schema(description, example)`.
95. Export a Postman Collection JSON (`commercex.postman_collection.json`) covering all endpoints grouped by service, including auth token chaining.
96. Write a `Dockerfile` for each service (multi-stage: `maven:3.9-eclipse-temurin-21` builder → `eclipse-temurin:21-jre` runner).
97. Write `docker-compose.yml` orchestrating all services + `postgres:16`, `redis:7`, `rabbitmq:3-management` with environment variables and health checks.
98. Create `.github/workflows/ci.yml` GitHub Actions pipeline: checkout → setup Java 21 (Temurin) → `mvn test` → `mvn package` → Docker build.
99. Write `@SpringBootTest` integration tests using Testcontainers (`PostgreSQLContainer`, `RedisContainer`, `RabbitMQContainer`) for order placement end-to-end flow.
100. Write `README.md` at project root: system architecture diagram (ASCII), local setup instructions, Docker Compose quickstart, and API reference summary.

---

## Relevant Files
- `commercex/pom.xml` — add all dependencies, convert to multi-module in Phase 7
- `src/main/resources/application.properties` — datasource, Redis, JWT, RabbitMQ config
- `src/main/java/com/commercex/CommercexApplication.java` — root entry point (until microservices split)
- New packages created per phase under `src/main/java/com/commercex/`

## Verification Per Phase
- Phase 1: `mvn test` passes; curl CRUD endpoints manually or with Postman
- Phase 2: POST `/auth/register` returns JWT; protected endpoints return 401/403 without token
- Phase 3: Cart add → order create flow works end-to-end; `@Transactional` rollback test passes
- Phase 4: Payment initiate → RabbitMQ message → order status update confirmed via GET
- Phase 5: Concurrent order test reproduces and handles optimistic lock; Redis cache hit confirmed via logs
- Phase 6: Ship status change → notification logged; full order lifecycle traceable
- Phase 7: All services register on Eureka; all routes accessible via API Gateway
- Phase 8: Swagger UI loads on each service; `docker-compose up` starts all containers; CI pipeline green

## Decisions / Scope
- Database: PostgreSQL (tasks reference it; H2 for tests)
- Message broker: RabbitMQ (simpler setup than Kafka for simulation)
- JWT library: `io.jsonwebtoken` (jjwt 0.12.x for Spring Boot 4 / Java 21)
- Caching: Redis with Spring Cache abstraction
- Service discovery: Eureka (Spring Cloud Netflix)
- Notifications: Simulated (log output only — no actual SMTP/SMS)
- Payment: Simulated gateway (no real payment provider)
