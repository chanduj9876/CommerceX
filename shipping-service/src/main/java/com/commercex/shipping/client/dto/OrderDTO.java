package com.commercex.shipping.client.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private Long userId;
    private String userEmail;
    private BigDecimal totalAmount;
    private String status;
}
