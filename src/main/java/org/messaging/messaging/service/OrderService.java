package org.messaging.messaging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.messaging.messaging.dto.OrderResponse;
import org.messaging.messaging.dto.PaymentMessage;
import org.messaging.messaging.dto.PaymentRequest;
import org.messaging.messaging.model.Order;
import org.messaging.messaging.model.OrderStatus;
import org.messaging.messaging.producer.PaymentProducer;
import org.messaging.messaging.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentProducer paymentProducer;

    /**
     * Cria o pedido com status PENDING e publica mensagem na fila de pagamento.
     */
    @Transactional
    public OrderResponse createAndProcessPayment(PaymentRequest request) {
        // Impede pedidos duplicados
        orderRepository.findByOrderNumber(request.getOrderNumber())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Pedido com número '%s' já existe.".formatted(request.getOrderNumber()));
                });

        Order order = Order.builder()
                .orderNumber(request.getOrderNumber())
                .cardNumber(request.getCardNumber())
                .cardHolder(request.getCardHolder())
                .cardExpiry(request.getCardExpiry())
                .cardCvv(request.getCardCvv())
                .amount(request.getAmount())
                .status(OrderStatus.PENDING)
                .build();

        order = orderRepository.save(order);
        log.info("[ORDER] Pedido criado: {} — status: {}", order.getOrderNumber(), order.getStatus());

        // Monta e publica a mensagem para o consumer de pagamento
        PaymentMessage message = new PaymentMessage();
        message.setOrderId(order.getId());
        message.setOrderNumber(order.getOrderNumber());
        message.setCardNumber(order.getCardNumber());
        message.setCardHolder(order.getCardHolder());
        message.setCardExpiry(order.getCardExpiry());
        message.setCardCvv(order.getCardCvv());
        message.setAmount(order.getAmount());

        paymentProducer.sendPaymentMessage(message);

        return toResponse(order);
    }

    /**
     * Consulta o pedido pelo número e retorna status + código de rastreio.
     */
    @Transactional(readOnly = true)
    public OrderResponse findByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pedido '%s' não encontrado.".formatted(orderNumber)));
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setCardHolder(order.getCardHolder());
        response.setAmount(order.getAmount());
        response.setStatus(order.getStatus());
        response.setTrackingCode(order.getTrackingCode());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }
}
