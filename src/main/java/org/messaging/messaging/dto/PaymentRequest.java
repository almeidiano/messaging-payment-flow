package org.messaging.messaging.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Payload recebido pelo endpoint POST /payments
 */
@Data
public class PaymentRequest {

    @NotBlank(message = "Número do pedido é obrigatório")
    private String orderNumber;

    @NotBlank(message = "Número do cartão é obrigatório")
    @Size(min = 13, max = 19, message = "Número do cartão deve ter entre 13 e 19 dígitos")
    private String cardNumber;

    @NotBlank(message = "Nome do portador é obrigatório")
    private String cardHolder;

    /** Formato MM/YY */
    @NotBlank(message = "Validade do cartão é obrigatória")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{2}$", message = "Validade deve estar no formato MM/YY")
    private String cardExpiry;

    @NotBlank(message = "CVV é obrigatório")
    @Size(min = 3, max = 4, message = "CVV deve ter 3 ou 4 dígitos")
    private String cardCvv;

    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser positivo")
    private Double amount;
}
