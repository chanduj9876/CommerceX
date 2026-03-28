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
import com.commercex.payment.mapper.PaymentMapper;
import com.commercex.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Payment service implementation.
 *
 * Flow:
 * 1. Validate the order exists and is in PENDING status
 * 2. Check no successful payment already exists for this order
 * 3. Create a PENDING payment record with a unique transactionId
 * 4. Route to the appropriate gateway adapter via PaymentGatewayFactory
 * 5. Update payment status based on gateway response (SUCCESS or FAILED)
 * 6. On SUCCESS: publish PaymentCompletedEvent via RabbitMQ
 * 7. The PaymentEventConsumer listens and updates Order status to CONFIRMED
 *
 * Why not update Order status directly in this service?
 * Event-driven architecture: the payment module publishes an event, and the
 * order module reacts independently. This loose coupling means:
 * - Payment doesn't need to know about Order internals
 * - Easy to add more reactions (send email, update analytics, etc.)
 * - Clear module boundaries for future microservice extraction
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentGatewayFactory gatewayFactory;
    private final PaymentEventProducer paymentEventProducer;

    @Override
    @Transactional
    public PaymentResponseDTO initiatePayment(Long userId, PaymentRequestDTO request) {
        // 1. Validate order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + request.getOrderId()));

        // Verify ownership
        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found with id: " + request.getOrderId());
        }

        // Only PENDING orders can be paid
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Cannot pay for order in " + order.getStatus() + " status. Only PENDING orders can be paid.");
        }

        // 2. Check for existing successful payment
        paymentRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.SUCCESS)
                .ifPresent(p -> {
                    throw new InvalidOrderStateException(
                            "Order " + order.getId() + " already has a successful payment (txn: " + p.getTransactionId() + ")");
                });

        // 3. Create PENDING payment
        String transactionId = UUID.randomUUID().toString();
        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .method(request.getMethod())
                .transactionId(transactionId)
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment {} initiated for order {} — method: {}, amount: {}",
                transactionId, order.getId(), request.getMethod(), order.getTotalAmount());

        // 4. Route to gateway
        PaymentGateway gateway = gatewayFactory.getGateway(request.getMethod());
        GatewayResponse response = gateway.processPayment(order.getTotalAmount(), transactionId);

        // 5. Update based on gateway response
        if (response.isSuccess()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setReferenceId(response.getReferenceId());
            log.info("Payment {} SUCCESS — gateway ref: {}", transactionId, response.getReferenceId());

            // 6. Publish event for order status update
            paymentEventProducer.publishPaymentCompleted(order.getId(), transactionId);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(response.getFailureReason());
            log.warn("Payment {} FAILED — reason: {}", transactionId, response.getFailureReason());
        }

        payment = paymentRepository.save(payment);
        return PaymentMapper.toResponseDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentsByOrderId(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Verify ownership
        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        return paymentRepository.findByOrderId(orderId).stream()
                .map(PaymentMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with transaction ID: " + transactionId));
        return PaymentMapper.toResponseDTO(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDTO confirmPayment(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with transaction ID: " + transactionId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Payment " + transactionId + " is already in " + payment.getStatus() + " status. Only PENDING payments can be confirmed.");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment = paymentRepository.save(payment);

        log.info("Payment {} confirmed via webhook/manual confirm", transactionId);

        // Publish event for order status update
        paymentEventProducer.publishPaymentCompleted(payment.getOrder().getId(), transactionId);

        return PaymentMapper.toResponseDTO(payment);
    }
}
