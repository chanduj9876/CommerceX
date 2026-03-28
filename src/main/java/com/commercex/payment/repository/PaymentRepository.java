package com.commercex.payment.repository;

import com.commercex.payment.entity.Payment;
import com.commercex.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
