package org.messaging.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

    // ─── Nomes das Exchanges ──────────────────────────────────────────────────
    public static final String PAYMENT_EXCHANGE     = "payment.exchange";
    public static final String LOGISTICS_EXCHANGE   = "logistics.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "dead.letter.exchange";

    // ─── Nomes das Filas ──────────────────────────────────────────────────────
    public static final String PAYMENT_QUEUE   = "payment.queue";
    public static final String LOGISTICS_QUEUE = "logistics.queue";
    public static final String PAYMENT_DLQ     = "payment.queue.dlq";
    public static final String LOGISTICS_DLQ   = "logistics.queue.dlq";

    // ─── Routing Keys ─────────────────────────────────────────────────────────
    public static final String PAYMENT_ROUTING_KEY   = "payment.process";
    public static final String LOGISTICS_ROUTING_KEY = "logistics.process";

    // ──────────────────────────────────────────────────────────────────────────
    // Exchanges
    // ──────────────────────────────────────────────────────────────────────────

    @Bean
    public DirectExchange paymentExchange() {
        return ExchangeBuilder.directExchange(PAYMENT_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange logisticsExchange() {
        return ExchangeBuilder.directExchange(LOGISTICS_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Filas principais (com DLQ configurada)
    // ──────────────────────────────────────────────────────────────────────────

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAYMENT_DLQ)
                .build();
    }

    @Bean
    public Queue logisticsQueue() {
        return QueueBuilder.durable(LOGISTICS_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", LOGISTICS_DLQ)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Dead Letter Queues
    // ──────────────────────────────────────────────────────────────────────────

    @Bean
    public Queue paymentDeadLetterQueue() {
        return QueueBuilder.durable(PAYMENT_DLQ).build();
    }

    @Bean
    public Queue logisticsDeadLetterQueue() {
        return QueueBuilder.durable(LOGISTICS_DLQ).build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Bindings
    // ──────────────────────────────────────────────────────────────────────────

    @Bean
    public Binding paymentBinding(Queue paymentQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentQueue).to(paymentExchange).with(PAYMENT_ROUTING_KEY);
    }

    @Bean
    public Binding logisticsBinding(Queue logisticsQueue, DirectExchange logisticsExchange) {
        return BindingBuilder.bind(logisticsQueue).to(logisticsExchange).with(LOGISTICS_ROUTING_KEY);
    }

    @Bean
    public Binding paymentDlqBinding(Queue paymentDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(paymentDeadLetterQueue).to(deadLetterExchange).with(PAYMENT_DLQ);
    }

    @Bean
    public Binding logisticsDlqBinding(Queue logisticsDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(logisticsDeadLetterQueue).to(deadLetterExchange).with(LOGISTICS_DLQ);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Converter JSON <-> Objeto Java
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * ObjectMapper do Jackson 3.x — suporte a java.time embutido, sem módulos extras.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return ObjectMapper.builder().build();
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}
