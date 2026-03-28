package com.commercex.shipping.dto;

import com.commercex.shipping.entity.ShipmentStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentResponseDTO {

    private Long id;
    private Long orderId;
    private String trackingId;
    private ShipmentStatus status;
    private LocalDate estimatedDelivery;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
