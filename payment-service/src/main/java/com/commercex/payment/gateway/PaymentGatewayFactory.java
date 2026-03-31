package com.commercex.payment.gateway;

import com.commercex.payment.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class PaymentGatewayFactory {

    private static final Map<PaymentMethod, Supplier<PaymentGateway>> registry = new EnumMap<>(PaymentMethod.class);

    static {
        registry.put(PaymentMethod.CREDIT_CARD, CreditCardGatewayAdapter::new);
        registry.put(PaymentMethod.UPI, UpiGatewayAdapter::new);
        registry.put(PaymentMethod.WALLET, WalletGatewayAdapter::new);
    }

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
