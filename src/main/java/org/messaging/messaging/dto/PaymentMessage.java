package org.messaging.messaging.dto;

import lombok.Data;

/**
 * Mensagem publicada na fila de autorização de pagamento.
 * Trafega entre o producer e o PaymentConsumer.
 */
@Data
public class PaymentMessage {
    private String orderId;
    private String orderNumber;
    private String cardNumber;
    private String cardHolder;
    private String cardExpiry;
    private String cardCvv;
    private Double amount;
}
