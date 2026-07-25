package org.messaging.messaging.model;

public enum OrderStatus {
    /** Pedido criado, aguardando autorização de pagamento */
    PENDING,

    /** Pagamento autorizado com sucesso pelo gateway */
    PAYMENT_APPROVED,

    /** Pagamento recusado pelo gateway */
    PAYMENT_DECLINED,

    /** Código de rastreio gerado pela logística */
    TRACKING_GENERATED,

    /** Falha ao gerar código de rastreio */
    LOGISTICS_FAILED
}
