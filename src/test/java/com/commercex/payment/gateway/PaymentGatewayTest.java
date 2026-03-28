package com.commercex.payment.gateway;

import com.commercex.payment.entity.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class PaymentGatewayTest {

    @Nested
    @DisplayName("CreditCardGatewayAdapter")
    class CreditCardTests {
        @RepeatedTest(5)
        @DisplayName("should return success or failure with correct format")
        void shouldProcessPayment() {
            CreditCardGatewayAdapter adapter = new CreditCardGatewayAdapter();
            GatewayResponse response = adapter.processPayment(new BigDecimal("100.00"), "txn-cc-test");

            if (response.isSuccess()) {
                assertThat(response.getReferenceId()).startsWith("CC-");
                assertThat(response.getFailureReason()).isNull();
            } else {
                assertThat(response.getFailureReason()).isNotBlank();
                assertThat(response.getReferenceId()).isNull();
            }
        }
    }

    @Nested
    @DisplayName("UpiGatewayAdapter")
    class UpiTests {
        @RepeatedTest(5)
        @DisplayName("should return success or failure with correct format")
        void shouldProcessPayment() {
            UpiGatewayAdapter adapter = new UpiGatewayAdapter();
            GatewayResponse response = adapter.processPayment(new BigDecimal("50.00"), "txn-upi-test");

            if (response.isSuccess()) {
                assertThat(response.getReferenceId()).startsWith("UPI-");
                assertThat(response.getFailureReason()).isNull();
            } else {
                assertThat(response.getFailureReason()).isNotBlank();
                assertThat(response.getReferenceId()).isNull();
            }
        }
    }

    @Nested
    @DisplayName("WalletGatewayAdapter")
    class WalletTests {
        @RepeatedTest(5)
        @DisplayName("should return success or failure with correct format")
        void shouldProcessPayment() {
            WalletGatewayAdapter adapter = new WalletGatewayAdapter();
            GatewayResponse response = adapter.processPayment(new BigDecimal("25.00"), "txn-wlt-test");

            if (response.isSuccess()) {
                assertThat(response.getReferenceId()).startsWith("WLT-");
                assertThat(response.getFailureReason()).isNull();
            } else {
                assertThat(response.getFailureReason()).isNotBlank();
                assertThat(response.getReferenceId()).isNull();
            }
        }
    }

    @Nested
    @DisplayName("PaymentGatewayFactory")
    class FactoryTests {
        private final PaymentGatewayFactory factory = new PaymentGatewayFactory();

        @Test
        @DisplayName("should return CreditCardGatewayAdapter for CREDIT_CARD")
        void shouldReturnCreditCardAdapter() {
            assertThat(factory.getGateway(PaymentMethod.CREDIT_CARD))
                    .isInstanceOf(CreditCardGatewayAdapter.class);
        }

        @Test
        @DisplayName("should return UpiGatewayAdapter for UPI")
        void shouldReturnUpiAdapter() {
            assertThat(factory.getGateway(PaymentMethod.UPI))
                    .isInstanceOf(UpiGatewayAdapter.class);
        }

        @Test
        @DisplayName("should return WalletGatewayAdapter for WALLET")
        void shouldReturnWalletAdapter() {
            assertThat(factory.getGateway(PaymentMethod.WALLET))
                    .isInstanceOf(WalletGatewayAdapter.class);
        }
    }

    @Nested
    @DisplayName("GatewayResponse")
    class GatewayResponseTests {
        @Test
        @DisplayName("success() should create a successful response")
        void shouldCreateSuccessResponse() {
            GatewayResponse response = GatewayResponse.success("REF-123");
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getReferenceId()).isEqualTo("REF-123");
            assertThat(response.getFailureReason()).isNull();
        }

        @Test
        @DisplayName("failure() should create a failed response")
        void shouldCreateFailureResponse() {
            GatewayResponse response = GatewayResponse.failure("Declined");
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getReferenceId()).isNull();
            assertThat(response.getFailureReason()).isEqualTo("Declined");
        }
    }
}
