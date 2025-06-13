package br.com.itau.secure.domain.service.status;

import br.com.itau.secure.api.client.RiskClient;
import br.com.itau.secure.api.model.*;
import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.service.SecureOrderService;
import br.com.itau.secure.infraestructure.rbbitmq.consumer.fake.PaymentSubscriptionFakeConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SecureOrderStatusService {
    public static final String ORDER_SECURE_STATUS_PROCESSING_KEY = "order.secure.status.processing";
    public static final String ORDER_SECURE_STATUS_PENDING_PAYMENT_SUBSCRIPTION_KEY = "order.secure.status.pending-payment-subscription";
    @Value("${rabbitmq.exchange}")
    private String exchangeName;
    @Value("${mock.fraud.id}")
    private String idOrderMockFraud;
    @Value("${mock.fraud.customer}")
    private String idCustomerMockFraud;
    private final SecureOrderService secureOrderService;
    private final RabbitTemplate rabbitTemplate;
    private final RiskClient riskClient;



    public SecureOrderStatusService(@Lazy SecureOrderService secureOrderService, RabbitTemplate rabbitTemplate, RiskClient riskClient) {
        this.secureOrderService = secureOrderService;
        this.rabbitTemplate = rabbitTemplate;
        this.riskClient = riskClient;
    }

    @Transactional
    public void processSecureOrder(OrderServiceStatusData orderServiceStatusData) {
        log.info("Processing secure order with orderId: {}, queue status: {}",orderServiceStatusData.orderId(), orderServiceStatusData.status());

        SecureOrder secureOrder = secureOrderService.findById(orderServiceStatusData.orderId());

        String eventType = orderServiceStatusData.status().toUpperCase();
        SecureOrderStatus currentStatus = secureOrder.getStatus();

        log.debug("Order {} is in state {}. Processing event type: {}", secureOrder.getId(), currentStatus, eventType);

        switch (currentStatus) {
            case RECEIVED:
                handleReceivedStateEvent(secureOrder, eventType, orderServiceStatusData);
                break;
            case VALIDATED:
                handleValidatedStateEvent(secureOrder, eventType, orderServiceStatusData);
                break;
            // somente pois nãao existe o serviço
            case PENDING:
                handlePendingStateEvent(secureOrder, eventType);
                break;
            case APPROVED:
            case REJECTED:
            case CANCELLED:
                handleTerminalStateEvent(secureOrder, eventType, orderServiceStatusData);
                break;
            default:
                log.warn("Order {} is in an unhandled status {} for event processing.", secureOrder.getId(), currentStatus);
                break;
        }

    }

    private void handleReceivedStateEvent(SecureOrder secureOrder, String eventType, OrderServiceStatusData data) {
        log.debug("Handling event '{}' for order {} in RECEIVED state.", eventType, secureOrder.getId());
        //Preciso editar pois os dados da api de fraude são provenientes de um mock então não uso os verdadeiros
        FraudCheckInput fraud;
        fraud = FraudCheckInput.builder()
                .orderId(idOrderMockFraud)
                .customerId(idCustomerMockFraud)
                .build();

        FraudCheckResult fraudCheckResult = riskClient.checkFraud(fraud);

        secureOrder.getStatus().moveToValidate(secureOrder, fraudCheckResult);
        SecureOrder secureOrderSaved = secureOrderService.createUpdateSecureOrder(secureOrder);
        sendStatusUpdateToQueueProcessing(secureOrderSaved,fraudCheckResult);

    }

    private void handleValidatedStateEvent(SecureOrder secureOrder, String eventType, OrderServiceStatusData data) {
        log.debug("Handling event '{}' for order {} in VALIDATED state.", eventType, secureOrder.getId());

        secureOrder.getStatus().moveToPending(data.fraudCheckResult(),secureOrder);
        SecureOrder secureOrderSaved = secureOrderService.createUpdateSecureOrder(secureOrder);
        sendStatusUpdateToQueuePaymentSubscription(secureOrderSaved);
        // como não temos os outros serviços, vamos simular o processamento externo e recebimento de outra fila aqui (: rs

        OrderServiceStatusData dataPending = OrderServiceStatusData.builder()
                .orderId(secureOrderSaved.getId())
                .status(secureOrderSaved.getStatus().toString())
                .build();
        processSecureOrder(dataPending);
    }

    private void handlePendingStateEvent(SecureOrder secureOrder, String eventType) {
        log.debug("Handling event '{}' for order {} in PENDING state.", eventType, secureOrder.getId());
        tryToApprovePendingOrder(secureOrder);
    }

    private void tryToApprovePendingOrder(SecureOrder secureOrder) {
        PaymentSubscriptionFakeConsumer.SimulatedProcessingOutput retornoFilasFake = PaymentSubscriptionFakeConsumer.simulateExternalProcessing(secureOrder.getId(), secureOrder.getStatus().toString());
        PaymentConfirmation payment = retornoFilasFake.paymentConfirmation();
        SubscriptionAuthorization subscription = retornoFilasFake.subscriptionAuthorization();

        log.info("All conditions met for order {}. Moving to approve.", secureOrder.getId());
        secureOrder.getStatus().moveToApprove(secureOrder, payment, subscription);
        secureOrderService.createUpdateSecureOrder(secureOrder);

        sendStatusUpdateToQueueProcessing(secureOrder, null);
    }

    private void handleTerminalStateEvent(SecureOrder secureOrder, String eventType, OrderServiceStatusData data) {
        log.debug("Handling event '{}' for order {} in TERMINAL state ({}).", eventType, secureOrder.getId(), secureOrder.getStatus());
        return;
    }

    public void sendStatusUpdateToQueueProcessing(SecureOrder secureOrder, FraudCheckResult fraudCheckResult) {
        log.info("Sending status update for order {} to queue with status {}", secureOrder.getId(), secureOrder.getStatus());

        OrderServiceStatusData statusData = new OrderServiceStatusData(secureOrder.getId(),secureOrder.getStatus().toString(),fraudCheckResult);
        rabbitTemplate.convertAndSend(exchangeName, ORDER_SECURE_STATUS_PROCESSING_KEY, statusData);
    }

    public void sendStatusUpdateToQueuePaymentSubscription(SecureOrder secureOrder) {
        log.info("Sending status update for order {} to queue payment and subsccription  with status {}", secureOrder.getId(), secureOrder.getStatus());

        OrderServiceStatusData statusData = new OrderServiceStatusData(secureOrder.getId(),secureOrder.getStatus().toString(), null);
        rabbitTemplate.convertAndSend(exchangeName, ORDER_SECURE_STATUS_PENDING_PAYMENT_SUBSCRIPTION_KEY, statusData);
    }

}
