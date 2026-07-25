package org.messaging.messaging.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

/**
 * Serviço de logística simulado (fake).
 * Gera um código de rastreio e tem 10% de chance de falha para simular cenários de erro.
 */
@Slf4j
@Service
public class FakeLogisticsService {

    private static final double SUCCESS_RATE = 0.90;
    private final Random random = new Random();

    /**
     * Solicita geração de código de rastreio ao serviço de logística.
     *
     * @param orderNumber número do pedido
     * @param recipient   nome do destinatário
     * @return código de rastreio ou {@code null} em caso de falha
     */
    public String generateTrackingCode(String orderNumber, String recipient) {
        simulateNetworkLatency();

        if (random.nextDouble() >= SUCCESS_RATE) {
            log.error("[LOGISTICS] Falha ao gerar rastreio para pedido {} / destinatário {}", orderNumber, recipient);
            return null;
        }

        // Formato: BR + 8 hex chars + BR (similar ao padrão Correios)
        String tracking = "BR" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase() + "BR";
        log.info("[LOGISTICS] Rastreio gerado: {} — pedido: {} | destinatário: {}", tracking, orderNumber, recipient);
        return tracking;
    }

    private void simulateNetworkLatency() {
        try {
            Thread.sleep(100 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
