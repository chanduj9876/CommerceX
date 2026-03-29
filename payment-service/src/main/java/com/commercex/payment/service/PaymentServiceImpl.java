package com.commercex.payment.service;

import com.commercex.common.InvalidOrderStateException;
import com.commercex.common.ResourceNotFoundException;
import com.commercex.payment.client.OrderDTO;
import com.commercex.payment.client.OrderServiceClient;
import com.commercex.payment.dto.PaymentRequestDTO;
import com.commercex.payment.dto.PaymentResponseDTO;
import com.commercex.payment.entity.Payment;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderServiceClient orderServiceClient;
    private final PaymentGatewayFactory gatewayFactory;
    private final PaymentEventProducer paymentEventProducer;

    @Override
    @Transactional
    public PaymentResponseDTO initiatePayment(Long userId, PaymentRequestDTO request) {
        // 1. Fetch order from order-service
        OrderDTO order = orderServiceClient.getOrder(request.getOrderId());

        // Verify ownership
        if (!order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found with id: " + request.getOrderId());
        }

        // Only PENDING orders can be paid
        if (!"PENDING".equals(order.getStatus())) {
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
                .orderId(order.getId())
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

            // 6. Publish event for order-service to update order status
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
        // Verify order exists and user owns it
        OrderDTO order = orderServiceClient.getOrder(orderId);
        if (!order.getUserId().equals(userId)) {
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

        // Publish event for order-service to update order status
        paymentEventProducer.publishPaymentCompleted(payment.getOrderId(), transactionId);

        return PaymentMapper.toResponseDTO(payment);
    }
}
