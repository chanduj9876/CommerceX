package com.commercex.payment.client;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {

    private Long id;
    private Long userId;
    private String userEmail;
    private BigDecimal totalAmount;
    private String status;
}
