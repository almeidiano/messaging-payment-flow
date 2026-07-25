package org.messaging.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.messaging.messaging.config.RabbitMQConfig;
import org.messaging.messaging.dto.LogisticsMessage;
import org.messaging.messaging.dto.PaymentMessage;
import org.messaging.messaging.model.Order;
import org.messaging.messaging.model.OrderStatus;
import org.messaging.messaging.producer.PaymentProducer;
import org.messaging.messaging.repository.OrderRepository;
import org.messaging.messaging.service.FakePaymentGatewayService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer da fila {@code payment.queue}.
 *
 * <p>Fluxo:
 * <ol>
 *   <li>Recebe a mensagem de pagamento</li>
 *   <li>Chama o gateway fake para autorizar</li>
 *   <li>Atualiza o pedido como PAYMENT_APPROVED ou PAYMENT_DECLINED</li>
 *   <li>Se aprovado, publica mensagem na fila de logística</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final OrderRepository orderRepository;
    private final FakePaymentGatewayService paymentGateway;
    private final PaymentProducer paymentProducer;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    @Transactional
    public void handlePaymentMessage(PaymentMessage message) {
        log.info("[PAYMENT-CONSUMER] Mensagem recebida → pedido: {} | valor: R$ {}",
                message.getOrderNumber(), message.getAmount());

        Order order = orderRepository.findById(message.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Pedido não encontrado para id: " + message.getOrderId()));

        // Chama o gateway fake
        boolean approved = paymentGateway.authorize(message.getCardNumber(), message.getAmount());

        if (approved) {
            order.setStatus(OrderStatus.PAYMENT_APPROVED);
            orderRepository.save(order);
            log.info("[PAYMENT-CONSUMER] Pedido {} marcado como PAYMENT_APPROVED.", order.getOrderNumber());

            // Publica na fila de logística
            LogisticsMessage logisticsMessage = new LogisticsMessage();
            logisticsMessage.setOrderId(order.getId());
            logisticsMessage.setOrderNumber(order.getOrderNumber());
            logisticsMessage.setCardHolder(order.getCardHolder());
            logisticsMessage.setAmount(order.getAmount());

            paymentProducer.sendLogisticsMessage(logisticsMessage);

        } else {
            order.setStatus(OrderStatus.PAYMENT_DECLINED);
            orderRepository.save(order);
            log.warn("[PAYMENT-CONSUMER] Pedido {} marcado como PAYMENT_DECLINED.", order.getOrderNumber());
        }
    }
}
