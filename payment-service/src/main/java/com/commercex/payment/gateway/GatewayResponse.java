package com.commercex.payment.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class GatewayResponse {

    private final boolean success;
    private final String referenceId;
    private final String failureReason;

    public static GatewayResponse success(String referenceId) {
        return new GatewayResponse(true, referenceId, null);
    }

    public static GatewayResponse failure(String reason) {
        return new GatewayResponse(false, null, reason);
    }
}
