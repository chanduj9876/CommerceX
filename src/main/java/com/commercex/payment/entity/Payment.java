package com.commercex.payment.entity;

import com.commercex.common.BaseEntity;
import com.commercex.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Payment entity — records each payment attempt against an order.
 *
 * Why a separate entity (not embedded in Order)?
 * - An order may have multiple payment attempts (first fails, second succeeds)
 * - Payment has its own lifecycle (PENDING → SUCCESS/FAILED → REFUNDED)
 * - Clean separation of concerns: order tracks what was bought, payment tracks how it was paid
 *
 * Why transactionId? A unique reference for each payment attempt, used to
 * confirm/query the payment. In a real system, this would come from the
 * payment gateway (e.g., Stripe charge ID). Here we generate a UUID.
 *
 * Why referenceId? An optional external reference from the gateway adapter
 * (e.g., bank authorization code). Useful for reconciliation.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order_id", columnList = "order_id"),
        @Index(name = "idx_payment_transaction_id", columnList = "transactionId", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(nullable = false, unique = true, length = 36)
    private String transactionId;

    private String referenceId;

    private String failureReason;
}
