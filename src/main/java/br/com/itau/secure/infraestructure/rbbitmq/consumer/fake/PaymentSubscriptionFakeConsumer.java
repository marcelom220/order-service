package br.com.itau.secure.infraestructure.rbbitmq.consumer.fake;

import br.com.itau.secure.api.model.PaymentConfirmation;
import br.com.itau.secure.api.model.SubscriptionAuthorization;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public class PaymentSubscriptionFakeConsumer {

    private static final Random random = new Random();

    public record SimulatedProcessingOutput(
            PaymentConfirmation paymentConfirmation,
            SubscriptionAuthorization subscriptionAuthorization
    ) {}

    public static SimulatedProcessingOutput simulateExternalProcessing(String orderId, String status) {
        log.info("Received fake payment and subscription event for orderId: {}, current status: {}", orderId, status);

        double chance = random.nextDouble();

        boolean paymentConfirmed;
        boolean subscriptionAuthorized;

        if (chance < 0.60) { // 60% Ambos confirmados/autorizados
            paymentConfirmed = true;
            subscriptionAuthorized = true;
        } else if (chance < 0.75) { // Proximos 15%  Pagamento negado, Subscrição autorizada
            paymentConfirmed = false;
            subscriptionAuthorized = true;
        } else if (chance < 0.90) { // Próximos 15%  Pagamento confirmado, Subscrição negada
            paymentConfirmed = true;
            subscriptionAuthorized = false;
        } else { // Últimos 10% ambos negados
            paymentConfirmed = false;
            subscriptionAuthorized = false;
        }
        log.info("Payment Status confirmed: {}, Subscription status confirmed: {}", paymentConfirmed, subscriptionAuthorized);
        return new SimulatedProcessingOutput(
                new PaymentConfirmation(paymentConfirmed),
                new SubscriptionAuthorization(subscriptionAuthorized)
        );
    }
}
