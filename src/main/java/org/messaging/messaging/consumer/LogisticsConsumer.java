package org.messaging.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.messaging.messaging.config.RabbitMQConfig;
import org.messaging.messaging.dto.LogisticsMessage;
import org.messaging.messaging.model.Order;
import org.messaging.messaging.model.OrderStatus;
import org.messaging.messaging.repository.OrderRepository;
import org.messaging.messaging.service.FakeLogisticsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer da fila {@code logistics.queue}.
 *
 * <p>Fluxo:
 * <ol>
 *   <li>Recebe a mensagem de logística (após pagamento aprovado)</li>
 *   <li>Chama o serviço fake de logística para gerar o código de rastreio</li>
 *   <li>Atualiza o pedido com o código de rastreio e status TRACKING_GENERATED</li>
 *   <li>Em caso de falha, marca como LOGISTICS_FAILED</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogisticsConsumer {

    private final OrderRepository orderRepository;
    private final FakeLogisticsService logisticsService;

    @RabbitListener(queues = RabbitMQConfig.LOGISTICS_QUEUE)
    @Transactional
    public void handleLogisticsMessage(LogisticsMessage message) {
        log.info("[LOGISTICS-CONSUMER] Mensagem recebida → pedido: {} | destinatário: {}",
                message.getOrderNumber(), message.getCardHolder());

        Order order = orderRepository.findById(message.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Pedido não encontrado para id: " + message.getOrderId()));

        String trackingCode = logisticsService.generateTrackingCode(
                message.getOrderNumber(), message.getCardHolder());

        if (trackingCode != null) {
            order.setTrackingCode(trackingCode);
            order.setStatus(OrderStatus.TRACKING_GENERATED);
            orderRepository.save(order);
            log.info("[LOGISTICS-CONSUMER] Pedido {} atualizado — rastreio: {}", order.getOrderNumber(), trackingCode);
        } else {
            order.setStatus(OrderStatus.LOGISTICS_FAILED);
            orderRepository.save(order);
            log.error("[LOGISTICS-CONSUMER] Falha ao gerar rastreio — pedido: {}", order.getOrderNumber());
        }
    }
}
