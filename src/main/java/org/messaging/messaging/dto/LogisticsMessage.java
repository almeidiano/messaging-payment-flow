package org.messaging.messaging.dto;

import lombok.Data;

/**
 * Mensagem publicada na fila de logística após pagamento aprovado.
 * Trafega entre o PaymentConsumer e o LogisticsConsumer.
 */
@Data
public class LogisticsMessage {
    private String orderId;
    private String orderNumber;
    private String cardHolder;
    private Double amount;
}
