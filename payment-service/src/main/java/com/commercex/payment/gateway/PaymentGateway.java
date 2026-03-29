package com.commercex.payment.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {

    GatewayResponse processPayment(BigDecimal amount, String transactionId);
}
