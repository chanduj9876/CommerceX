package com.commercex.payment.service;

import com.commercex.common.InvalidOrderStateException;
import com.commercex.common.ResourceNotFoundException;
import com.commercex.order.entity.Order;
import com.commercex.order.entity.OrderStatus;
import com.commercex.order.repository.OrderRepository;
import com.commercex.payment.dto.PaymentRequestDTO;
import com.commercex.payment.dto.PaymentResponseDTO;
import com.commercex.payment.entity.Payment;
import com.commercex.payment.entity.PaymentMethod;
import com.commercex.payment.entity.PaymentStatus;
import com.commercex.payment.event.PaymentEventProducer;
import com.commercex.payment.gateway.GatewayResponse;
import com.commercex.payment.gateway.PaymentGateway;
import com.commercex.payment.gateway.PaymentGatewayFactory;
import com.commercex.payment.repository.PaymentRepository;
import com.commercex.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentGatewayFactory gatewayFactory;
    @Mock private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User testUser;
    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        pendingOrder = Order.builder()
                .user(testUser)
                .totalAmount(new BigDecimal("199.99"))
                .status(OrderStatus.PENDING)
                .build();
        pendingOrder.setId(100L);
        pendingOrder.setCreatedAt(LocalDateTime.now());
        pendingOrder.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("initiatePayment")
    class InitiatePayment {

        @Test
        @DisplayName("should process payment successfully via credit card")
        void shouldProcessPaymentSuccessfully() {
            PaymentRequestDTO request = PaymentRequestDTO.builder()
                    .orderId(100L)
                    .method(PaymentMethod.CREDIT_CARD)
                    .build();

            PaymentGateway mockGateway = mock(PaymentGateway.class);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));
            when(paymentRepository.findByOrderIdAndStatus(100L, PaymentStatus.SUCCESS))
                    .thenReturn(Optional.empty());
            when(gatewayFactory.getGateway(PaymentMethod.CREDIT_CARD)).thenReturn(mockGateway);
            when(mockGateway.processPayment(any(), any()))
                    .thenReturn(GatewayResponse.success("CC-REF123"));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(1L);
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                return p;
            });

            PaymentResponseDTO result = paymentService.initiatePayment(1L, request);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(result.getReferenceId()).isEqualTo("CC-REF123");
            assertThat(result.getAmount()).isEqualByComparingTo("199.99");
            assertThat(result.getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
            verify(paymentEventProducer).publishPaymentCompleted(eq(100L), anyString());
        }

        @Test
        @DisplayName("should handle gateway failure")
        void shouldHandleGatewayFailure() {
            PaymentRequestDTO request = PaymentRequestDTO.builder()
                    .orderId(100L)
                    .method(PaymentMethod.UPI)
                    .build();

            PaymentGateway mockGateway = mock(PaymentGateway.class);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));
            when(paymentRepository.findByOrderIdAndStatus(100L, PaymentStatus.SUCCESS))
                    .thenReturn(Optional.empty());
            when(gatewayFactory.getGateway(PaymentMethod.UPI)).thenReturn(mockGateway);
            when(mockGateway.processPayment(any(), any()))
                    .thenReturn(GatewayResponse.failure("UPI timeout"));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(2L);
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                return p;
            });

            PaymentResponseDTO result = paymentService.initiatePayment(1L, request);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(result.getFailureReason()).isEqualTo("UPI timeout");
            verify(paymentEventProducer, never()).publishPaymentCompleted(anyLong(), anyString());
        }

        @Test
        @DisplayName("should throw when order not found")
        void shouldThrowWhenOrderNotFound() {
            PaymentRequestDTO request = PaymentRequestDTO.builder()
                    .orderId(999L)
                    .method(PaymentMethod.WALLET)
                    .build();
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.initiatePayment(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("should throw when user doesn't own the order")
        void shouldThrowWhenUserDoesNotOwnOrder() {
            PaymentRequestDTO request = PaymentRequestDTO.builder()
                    .orderId(100L)
                    .method(PaymentMethod.CREDIT_CARD)
                    .build();
            when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));

            // User 99 trying to pay for user 1's order
            assertThatThrownBy(() -> paymentService.initiatePayment(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when order is not PENDING")
        void shouldThrowWhenOrderNotPending() {
            pendingOrder.setStatus(OrderStatus.CONFIRMED);
            PaymentRequestDTO request = PaymentRequestDTO.builder()
                    .orderId(100L)
                    .method(PaymentMethod.CREDIT_CARD)
                    .build();
            when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));

            assertThatThrownBy(() -> paymentService.initiatePayment(1L, request))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("CONFIRMED");
        }

        @Test
        @DisplayName("should throw when order already has successful payment")
        void shouldThrowWhenAlreadyPaid() {
            PaymentRequestDTO request = PaymentRequestDTO.builder()
                    .orderId(100L)
                    .method(PaymentMethod.CREDIT_CARD)
                    .build();

            Payment existingPayment = Payment.builder()
                    .transactionId("existing-txn")
                    .status(PaymentStatus.SUCCESS)
                    .build();

            when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));
            when(paymentRepository.findByOrderIdAndStatus(100L, PaymentStatus.SUCCESS))
                    .thenReturn(Optional.of(existingPayment));

            assertThatThrownBy(() -> paymentService.initiatePayment(1L, request))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("already has a successful payment");
        }

        @Test
        @DisplayName("should use wallet gateway for WALLET method")
        void shouldUseWalletGateway() {
            PaymentRequestDTO request = PaymentRequestDTO.builder()
                    .orderId(100L)
                    .method(PaymentMethod.WALLET)
                    .build();

            PaymentGateway mockGateway = mock(PaymentGateway.class);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));
            when(paymentRepository.findByOrderIdAndStatus(100L, PaymentStatus.SUCCESS))
                    .thenReturn(Optional.empty());
            when(gatewayFactory.getGateway(PaymentMethod.WALLET)).thenReturn(mockGateway);
            when(mockGateway.processPayment(any(), any()))
                    .thenReturn(GatewayResponse.success("WLT-REF456"));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(3L);
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                return p;
            });

            PaymentResponseDTO result = paymentService.initiatePayment(1L, request);

            assertThat(result.getMethod()).isEqualTo(PaymentMethod.WALLET);
            verify(gatewayFactory).getGateway(PaymentMethod.WALLET);
        }
    }

    @Nested
    @DisplayName("getPaymentsByOrderId")
    class GetPaymentsByOrderId {

        @Test
        @DisplayName("should return payments for user's order")
        void shouldReturnPayments() {
            Payment payment = Payment.builder()
                    .order(pendingOrder)
                    .amount(new BigDecimal("199.99"))
                    .status(PaymentStatus.SUCCESS)
                    .method(PaymentMethod.CREDIT_CARD)
                    .transactionId("txn-123")
                    .referenceId("CC-REF")
                    .build();
            payment.setId(1L);
            payment.setCreatedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());

            when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));
            when(paymentRepository.findByOrderId(100L)).thenReturn(List.of(payment));

            List<PaymentResponseDTO> result = paymentService.getPaymentsByOrderId(100L, 1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTransactionId()).isEqualTo("txn-123");
        }

        @Test
        @DisplayName("should throw when user doesn't own the order")
        void shouldThrowForWrongUser() {
            when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));

            assertThatThrownBy(() -> paymentService.getPaymentsByOrderId(100L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when order not found")
        void shouldThrowWhenOrderNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentsByOrderId(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getPaymentByTransactionId")
    class GetPaymentByTransactionId {

        @Test
        @DisplayName("should return payment by transaction ID")
        void shouldReturnPayment() {
            Payment payment = Payment.builder()
                    .order(pendingOrder)
                    .amount(new BigDecimal("199.99"))
                    .status(PaymentStatus.SUCCESS)
                    .method(PaymentMethod.UPI)
                    .transactionId("txn-456")
                    .build();
            payment.setId(2L);
            payment.setCreatedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());

            when(paymentRepository.findByTransactionId("txn-456")).thenReturn(Optional.of(payment));

            PaymentResponseDTO result = paymentService.getPaymentByTransactionId("txn-456");

            assertThat(result.getTransactionId()).isEqualTo("txn-456");
            assertThat(result.getMethod()).isEqualTo(PaymentMethod.UPI);
        }

        @Test
        @DisplayName("should throw when transaction not found")
        void shouldThrowWhenNotFound() {
            when(paymentRepository.findByTransactionId("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentByTransactionId("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("nonexistent");
        }
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPayment {

        @Test
        @DisplayName("should confirm a PENDING payment and publish event")
        void shouldConfirmPendingPayment() {
            Payment pendingPayment = Payment.builder()
                    .order(pendingOrder)
                    .amount(new BigDecimal("199.99"))
                    .status(PaymentStatus.PENDING)
                    .method(PaymentMethod.CREDIT_CARD)
                    .transactionId("txn-pending")
                    .build();
            pendingPayment.setId(10L);
            pendingPayment.setCreatedAt(LocalDateTime.now());
            pendingPayment.setUpdatedAt(LocalDateTime.now());

            when(paymentRepository.findByTransactionId("txn-pending"))
                    .thenReturn(Optional.of(pendingPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponseDTO result = paymentService.confirmPayment("txn-pending");

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            verify(paymentEventProducer).publishPaymentCompleted(eq(100L), eq("txn-pending"));
        }

        @Test
        @DisplayName("should throw when payment not found")
        void shouldThrowWhenPaymentNotFound() {
            when(paymentRepository.findByTransactionId("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.confirmPayment("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("nonexistent");
        }

        @Test
        @DisplayName("should throw when payment is not PENDING")
        void shouldThrowWhenNotPending() {
            Payment successPayment = Payment.builder()
                    .order(pendingOrder)
                    .amount(new BigDecimal("199.99"))
                    .status(PaymentStatus.SUCCESS)
                    .method(PaymentMethod.UPI)
                    .transactionId("txn-success")
                    .build();
            successPayment.setId(11L);

            when(paymentRepository.findByTransactionId("txn-success"))
                    .thenReturn(Optional.of(successPayment));

            assertThatThrownBy(() -> paymentService.confirmPayment("txn-success"))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("SUCCESS");
        }
    }
}
