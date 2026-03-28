package com.commercex.payment.gateway;

import com.commercex.payment.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry-based Factory pattern — returns the correct PaymentGateway adapter
 * based on PaymentMethod.
 *
 * Why registry instead of switch? Open/Closed Principle (OCP).
 * Adding a new payment method (e.g., PAYPAL) only requires:
 *   1. Create PayPalGatewayAdapter implements PaymentGateway
 *   2. Call registerGateway(PaymentMethod.PAYPAL, PayPalGatewayAdapter::new)
 * No modification to this class needed.
 *
 * Why Supplier<PaymentGateway>? Each call creates a fresh adapter instance,
 * avoiding shared mutable state between concurrent requests.
 */
@Component
public class PaymentGatewayFactory {

    private static final Map<PaymentMethod, Supplier<PaymentGateway>> registry = new EnumMap<>(PaymentMethod.class);

    static {
        registry.put(PaymentMethod.CREDIT_CARD, CreditCardGatewayAdapter::new);
        registry.put(PaymentMethod.UPI, UpiGatewayAdapter::new);
        registry.put(PaymentMethod.WALLET, WalletGatewayAdapter::new);
    }

    /**
     * Register a new payment gateway at runtime (OCP — extend without modifying).
     */
    public static void registerGateway(PaymentMethod method, Supplier<PaymentGateway> creator) {
        registry.put(method, creator);
    }

    public PaymentGateway getGateway(PaymentMethod method) {
        Supplier<PaymentGateway> creator = registry.get(method);
        if (creator == null) {
            throw new IllegalArgumentException("No gateway registered for payment method: " + method);
        }
        return creator.get();
    }
}
