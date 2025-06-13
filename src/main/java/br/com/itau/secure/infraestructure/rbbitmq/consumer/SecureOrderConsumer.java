package br.com.itau.secure.infraestructure.rbbitmq.consumer;

import br.com.itau.secure.api.model.OrderServiceStatusData;
import br.com.itau.secure.domain.service.status.SecureOrderStatusService;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecureOrderConsumer {


    private final SecureOrderStatusService secureOrderStatusService;

    public SecureOrderConsumer(SecureOrderStatusService secureOrderStatusService) {
        this.secureOrderStatusService = secureOrderStatusService;
    }

    @RabbitListener(queues = "${rabbitmq.process-order.queue}", concurrency = "2-3")
    @SneakyThrows
    public void handleProcessingProcessOrder(@Payload OrderServiceStatusData orderServiceStatusData) {
        secureOrderStatusService.processSecureOrder(orderServiceStatusData);
//        Thread.sleep(Duration.ofSeconds(5));
    }
}
