package org.messaging.messaging.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.messaging.messaging.config.RabbitMQConfig;
import org.messaging.messaging.dto.LogisticsMessage;
import org.messaging.messaging.dto.PaymentMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publica mensagem de autorização de pagamento na fila {@code payment.queue}.
     */
    public void sendPaymentMessage(PaymentMessage message) {
        log.info("[PRODUCER] Publicando mensagem de pagamento → pedido: {}", message.getOrderNumber());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_ROUTING_KEY,
                message
        );
        log.debug("[PRODUCER] Mensagem publicada: {}", message);
    }

    /**
     * Publica mensagem de logística na fila {@code logistics.queue} após pagamento aprovado.
     */
    public void sendLogisticsMessage(LogisticsMessage message) {
        log.info("[PRODUCER] Publicando mensagem de logística → pedido: {}", message.getOrderNumber());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.LOGISTICS_EXCHANGE,
                RabbitMQConfig.LOGISTICS_ROUTING_KEY,
                message
        );
        log.debug("[PRODUCER] Mensagem publicada: {}", message);
    }
}
