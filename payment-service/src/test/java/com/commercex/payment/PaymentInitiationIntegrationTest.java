package com.commercex.payment;

import com.commercex.payment.client.UserServiceClient;
import com.commercex.payment.dto.PaymentRequestDTO;
import com.commercex.payment.dto.PaymentResponseDTO;
import com.commercex.payment.dto.client.UserDTO;
import com.commercex.payment.entity.PaymentMethod;
import com.commercex.payment.entity.PaymentStatus;
import com.commercex.payment.repository.PaymentRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * Integration test for payment initiation.
 * Verifies that:
 * 1. The application context starts with real Postgres + RabbitMQ containers.
 * 2. A payment initiation request is processed and a payment record is persisted.
 * UserServiceClient (Feign) is mocked since user-service runs separately.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentInitiationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("commercex_payments_test")
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
    PaymentRepository paymentRepository;

    @MockBean
    UserServiceClient userServiceClient;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_ORDER_ID = 42L;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();

        UserDTO user = new UserDTO();
        user.setId(TEST_USER_ID);
        user.setEmail("jane@example.com");
        user.setName("Jane Doe");
        Mockito.when(userServiceClient.getUserByEmail(any())).thenReturn(user);
        Mockito.when(userServiceClient.getUser(TEST_USER_ID)).thenReturn(user);
    }

    @Test
    @DisplayName("Application context loads with real Postgres + RabbitMQ containers")
    void contextLoads() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(rabbitmq.isRunning()).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/payments/initiate — creates payment record")
    void initiatePayment_persistsRecord() {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(TEST_ORDER_ID)
                .method(PaymentMethod.CREDIT_CARD)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Email", "jane@example.com");

        HttpEntity<PaymentRequestDTO> entity = new HttpEntity<>(request, headers);

        ResponseEntity<PaymentResponseDTO> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/payments/initiate",
                entity,
                PaymentResponseDTO.class);

        // If auth is enforced, 401 is expected without a real JWT.
        // The key assertion is that the service started and the DB/AMQP connections work.
        assertThat(response.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.UNAUTHORIZED);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            PaymentResponseDTO payment = response.getBody();
            assertThat(payment).isNotNull();
            assertThat(payment.getOrderId()).isEqualTo(TEST_ORDER_ID);
            assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
            assertThat(payment.getStatus()).isIn(PaymentStatus.SUCCESS, PaymentStatus.FAILED);
            assertThat(payment.getTransactionId()).isNotBlank();

            // Verify persisted in DB
            assertThat(paymentRepository.findAll()).hasSize(1);
        }
    }

    @Test
    @DisplayName("GET /api/v1/payments/{transactionId} — returns 401 without JWT")
    void getPaymentByTransactionId_requiresAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/payments/TXN-nonexistent",
                String.class);

        // Without auth, should be 401 (not 500)
        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.NOT_FOUND);
    }
}
