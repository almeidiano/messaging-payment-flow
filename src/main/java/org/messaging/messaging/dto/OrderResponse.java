package org.messaging.messaging.dto;

import lombok.Data;
import org.messaging.messaging.model.OrderStatus;

import java.time.LocalDateTime;

/**
 * Resposta do endpoint GET /orders/{orderNumber}
 */
@Data
public class OrderResponse {
    private String id;
    private String orderNumber;
    private String cardHolder;
    private Double amount;
    private OrderStatus status;
    private String trackingCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
