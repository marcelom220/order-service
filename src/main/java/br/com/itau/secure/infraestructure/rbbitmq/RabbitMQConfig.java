package br.com.itau.secure.infraestructure.rbbitmq;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_SECURE_STATUS_PROCESSING_KEY = "order.secure.status.processing";
    public static final String ORDER_SECURE_STATUS_PENDING_PAYMENT_SUBSCRIPTION_KEY = "order.secure.status.pending-payment-subscription";
    @Value("${rabbitmq.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.process-order.queue}")
    private String queueProcessOrder;

    @Value("${rabbitmq.process-order.dlq}")
    private String deadLetterQueueProcessOrder;

    @Value("${rabbitmq.process-payment-subscription.queue}")
    private String queueProcessPaymentSubscription;

    @Value("${rabbitmq.process-payment-subscription.dlq}")
    private String deadLetterQueueProcessPaymentSubscription;


    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Queue queueProcessOrder() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", deadLetterQueueProcessOrder);
        return QueueBuilder.durable(queueProcessOrder).withArguments(args).build();
    }

    @Bean
    public Queue deadLetterQueueProcessOrder() {
        return QueueBuilder.durable(deadLetterQueueProcessOrder).build();
    }

    @Bean
    public TopicExchange exchange() {
        return ExchangeBuilder.topicExchange(exchangeName).build();
    }

    @Bean
    public Binding bindingProcess() {
        return BindingBuilder.bind(queueProcessOrder()).to(exchange())
                .with(ORDER_SECURE_STATUS_PROCESSING_KEY);
    }

    @Bean
    public Queue queueProcessPaymentSubscription() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", deadLetterQueueProcessPaymentSubscription);
        return QueueBuilder.durable(queueProcessPaymentSubscription).withArguments(args).build();
    }

    @Bean
    public Queue deadLetterQueuePaymentSubscription() {
        return QueueBuilder.durable(deadLetterQueueProcessPaymentSubscription).build();
    }

    @Bean
    public Binding bindingProcessPendentePagamentoSubscricao() {
        return BindingBuilder.bind(queueProcessOrder()).to(exchange())
                .with(ORDER_SECURE_STATUS_PENDING_PAYMENT_SUBSCRIPTION_KEY);
    }


}
