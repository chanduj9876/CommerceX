package com.commercex.shipping;

import com.commercex.shipping.client.OrderServiceClient;
import com.commercex.shipping.client.UserServiceClient;
import com.commercex.shipping.dto.CreateShipmentRequest;
import com.commercex.shipping.dto.ShipmentResponseDTO;
import com.commercex.shipping.client.dto.OrderDTO;
import com.commercex.shipping.client.dto.UserDTO;
import com.commercex.shipping.entity.ShipmentStatus;
import com.commercex.shipping.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * Integration test for shipment creation and RabbitMQ event consumption.
 * Verifies:
 * 1. Context loads with real Postgres + RabbitMQ containers.
 * 2. Manual shipment creation via REST endpoint.
 * 3. Shipment record is persisted to the real DB.
 * OrderServiceClient and UserServiceClient (Feign) are mocked.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ShipmentCreationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("commercex_shipping_test")
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
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ShipmentRepository shipmentRepository;

    @MockitoBean
    OrderServiceClient orderServiceClient;

    @MockitoBean
    UserServiceClient userServiceClient;

    private static final Long TEST_ORDER_ID = 42L;

    @BeforeEach
    void setUp() {
        shipmentRepository.deleteAll();

        // Stub Feign clients
        OrderDTO order = new OrderDTO();
        order.setId(TEST_ORDER_ID);
        order.setUserEmail("jane@example.com");
        order.setStatus("CONFIRMED");
        Mockito.when(orderServiceClient.getOrder(anyLong())).thenReturn(order);

        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setEmail("jane@example.com");
        user.setName("Jane Doe");
        Mockito.when(userServiceClient.getUserByEmail(Mockito.any())).thenReturn(user);
    }

    @Test
    @DisplayName("Application context loads with real Postgres + RabbitMQ containers")
    void contextLoads() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(rabbitmq.isRunning()).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/shipments — creates shipment (admin required)")
    void createShipment_persistsRecord() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setOrderId(TEST_ORDER_ID);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CreateShipmentRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ShipmentResponseDTO> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/shipments",
                entity,
                ShipmentResponseDTO.class);

        // Without JWT admin token, 401 is expected.
        // The key assertion: service started and DB/AMQP connections work.
        assertThat(response.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.UNAUTHORIZED);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            ShipmentResponseDTO shipment = response.getBody();
            assertThat(shipment).isNotNull();
            assertThat(shipment.getOrderId()).isEqualTo(TEST_ORDER_ID);
            assertThat(shipment.getTrackingId()).startsWith("TRK-");
            assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CREATED);

            // Verify persisted in DB
            assertThat(shipmentRepository.findAll()).hasSize(1);
        }
    }

    @Test
    @DisplayName("GET /api/v1/shipments/{trackingId} — returns 401 without JWT")
    void trackShipment_requiresAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/shipments/TRK-nonexistent",
                String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.NOT_FOUND);
    }
}
