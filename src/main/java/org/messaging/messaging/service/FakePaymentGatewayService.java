package org.messaging.messaging.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Gateway de pagamento simulado (fake).
 * Aprova 80% das transações para simular um cenário realista.
 */
@Slf4j
@Service
public class FakePaymentGatewayService {

    private static final double APPROVAL_RATE = 0.80;
    private final Random random = new Random();

    /**
     * Tenta autorizar o pagamento com os dados do cartão informados.
     *
     * @param cardNumber número do cartão
     * @param amount     valor a ser cobrado
     * @return {@code true} se autorizado, {@code false} se recusado
     */
    public boolean authorize(String cardNumber, Double amount) {
        // Simula latência de um gateway real (50–300 ms)
        simulateNetworkLatency();

        // Cartão com número terminado em 0000 → sempre recusado (útil para testes)
        if (cardNumber.endsWith("0000")) {
            log.warn("[GATEWAY] Cartão {} recusado por regra de bloqueio. Valor: R$ {}", maskCard(cardNumber), amount);
            return false;
        }

        boolean approved = random.nextDouble() < APPROVAL_RATE;
        if (approved) {
            log.info("[GATEWAY] Pagamento APROVADO — cartão {} | valor: R$ {}", maskCard(cardNumber), amount);
        } else {
            log.warn("[GATEWAY] Pagamento RECUSADO — cartão {} | valor: R$ {}", maskCard(cardNumber), amount);
        }
        return approved;
    }

    private void simulateNetworkLatency() {
        try {
            Thread.sleep(50 + random.nextInt(250));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Mascara o cartão exibindo apenas os 4 últimos dígitos */
    private String maskCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
