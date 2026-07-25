package org.messaging.messaging.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.messaging.messaging.dto.OrderResponse;
import org.messaging.messaging.dto.PaymentRequest;
import org.messaging.messaging.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints REST do fluxo de pagamento.
 *
 * <pre>
 * POST /payments          → inicia o processamento de pagamento
 * GET  /orders/{number}   → consulta pedido por número
 * </pre>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;

    /**
     * Recebe os dados do cartão + número do pedido,
     * persiste o pedido e dispara o fluxo de mensageria.
     *
     * @param request payload com dados do cartão e pedido
     * @return pedido criado com status PENDING
     */
    @PostMapping("/payments")
    public ResponseEntity<OrderResponse> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        log.info("[CONTROLLER] Requisição de pagamento recebida → pedido: {}", request.getOrderNumber());
        OrderResponse response = orderService.createAndProcessPayment(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Consulta o pedido pelo número e retorna seu status atual,
     * incluindo o código de rastreio (quando disponível).
     *
     * @param orderNumber número único do pedido
     * @return detalhes do pedido
     */
    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderNumber) {
        log.info("[CONTROLLER] Consulta de pedido → número: {}", orderNumber);
        OrderResponse response = orderService.findByOrderNumber(orderNumber);
        return ResponseEntity.ok(response);
    }
}
