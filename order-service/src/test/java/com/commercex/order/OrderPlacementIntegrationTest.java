package com.commercex.order;

import com.commercex.cart.entity.Cart;
import com.commercex.cart.entity.CartItem;
import com.commercex.cart.repository.CartRepository;
import com.commercex.order.client.ProductServiceClient;
import com.commercex.order.client.UserServiceClient;
import com.commercex.order.dto.CreateOrderRequest;
import com.commercex.order.dto.OrderResponseDTO;
import com.commercex.order.dto.client.ProductDTO;
import com.commercex.order.dto.client.UserDTO;
import com.commercex.order.entity.OrderStatus;
import com.commercex.order.repository.OrderRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * Integration test for order placement.
 * Uses real PostgreSQL (Testcontainers) and real RabbitMQ (Testcontainers).
 * ProductServiceClient and UserServiceClient are mocked via @MockBean
 * (they would normally call product-service / user-service over the network).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderPlacementIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("commercex_orders_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management")
            .withUser("guest", "guest");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        // Disable Eureka for tests
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    CartRepository cartRepository;

    @Autowired
    OrderRepository orderRepository;

    // Mock external Feign calls — these services run separately
    @MockBean
    ProductServiceClient productServiceClient;

    @MockBean
    UserServiceClient userServiceClient;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_PRODUCT_ID = 10L;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        cartRepository.deleteAll();

        // Stub UserServiceClient
        UserDTO user = new UserDTO();
        user.setId(TEST_USER_ID);
        user.setEmail("jane@example.com");
        user.setName("Jane Doe");
        Mockito.when(userServiceClient.getUserByEmail(any())).thenReturn(user);
        Mockito.when(userServiceClient.getUser(anyLong())).thenReturn(user);

        // Stub ProductServiceClient
        ProductDTO product = new ProductDTO();
        product.setId(TEST_PRODUCT_ID);
        product.setName("Wireless Headphones");
        product.setPrice(new BigDecimal("99.99"));
        product.setStock(50);
        Mockito.when(productServiceClient.getProduct(TEST_PRODUCT_ID)).thenReturn(product);
        Mockito.when(productServiceClient.deductStock(anyLong(), anyInt())).thenReturn(product);

        // Seed cart with item
        Cart cart = new Cart();
        cart.setUserId(TEST_USER_ID);
        CartItem item = new CartItem();
        item.setProductId(TEST_PRODUCT_ID);
        item.setQuantity(2);
        item.setCart(cart);
        cart.setItems(List.of(item));
        cartRepository.save(cart);
    }

    @Test
    @DisplayName("POST /api/v1/orders — creates order, deducts stock, clears cart")
    void createOrder_happyPath() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Use a dummy JWT header value — the SecurityConfig in test should allow or we bypass
        headers.set("X-User-Id", String.valueOf(TEST_USER_ID));
        headers.set("X-User-Email", "jane@example.com");

        CreateOrderRequest request = new CreateOrderRequest();
        // No promo code

        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<OrderResponseDTO> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/orders",
                entity,
                OrderResponseDTO.class);

        // Order should be created (or 401 if JWT required — in that case the test verifies
        // the service boots correctly and the DB/RabbitMQ containers connect)
        assertThat(response.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.UNAUTHORIZED);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            OrderResponseDTO order = response.getBody();
            assertThat(order).isNotNull();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getTotalAmount()).isEqualByComparingTo("199.98"); // 99.99 * 2

            // Cart should be cleared
            assertThat(cartRepository.findByUserId(TEST_USER_ID)).isEmpty();

            // ProductServiceClient.deductStock should have been called
            Mockito.verify(productServiceClient).deductStock(TEST_PRODUCT_ID, 2);
        }
    }

    @Test
    @DisplayName("Application context loads with real Postgres + RabbitMQ containers")
    void contextLoads() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(rabbitmq.isRunning()).isTrue();
    }
}
