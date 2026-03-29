package com.commercex.payment.event;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent implements Serializable {

    private Long orderId;
    private String transactionId;
}
