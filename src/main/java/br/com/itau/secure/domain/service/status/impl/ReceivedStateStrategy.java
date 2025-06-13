package br.com.itau.secure.domain.service.status.impl;

import br.com.itau.secure.api.model.FraudCheckResult;
import br.com.itau.secure.api.model.PaymentConfirmation;
import br.com.itau.secure.api.model.SubscriptionAuthorization;
import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.service.status.SecureOrderStateStrategy;
import br.com.itau.secure.domain.service.status.SecureOrderStatus;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
public class ReceivedStateStrategy implements SecureOrderStateStrategy {
    @Override
    public void moveToValidate(SecureOrder secureOrder, FraudCheckResult fraudCheckResult) {
        log.info("Validating PolicyRequest {} from RECEIVED state. FraudCheckResult: {}", secureOrder.getId(), fraudCheckResult);

        if (fraudCheckResult == null || fraudCheckResult.classification() == null || fraudCheckResult.classification().isBlank()) {
            log.warn("Fraud check result is inconclusive or missing for order {}. Rejecting.", secureOrder.getId());
            secureOrder.setStatus(SecureOrderStatus.REJECTED, Instant.now());
            return;
        }

        String riskClassification = fraudCheckResult.classification();
        log.info("Order {} received fraud classification: {}. Storing classification and moving to VALIDATED.", secureOrder.getId(), riskClassification);

        secureOrder.setStatus(SecureOrderStatus.VALIDATED, Instant.now());
        log.info("Order {} successfully moved to VALIDATED state.", secureOrder.getId());

    }

    @Override
    public void moveToPending(FraudCheckResult fraudCheckResult, SecureOrder secureOrder) {
        throw new UnsupportedOperationException("Cannot move to PENDING directly from RECEIVED without validation.");
    }
    @Override
    public void moveToApprove(SecureOrder secureOrder, PaymentConfirmation payment, SubscriptionAuthorization subscription) {
        throw new UnsupportedOperationException("Cannot approve directly from RECEIVED state.");
    }

    @Override
    public void moveToReject(SecureOrder secureOrder) {
        secureOrder.setStatus(SecureOrderStatus.REJECTED, Instant.now());
    }

    @Override
    public void moveToCancel(SecureOrder secureOrder) {
        secureOrder.setStatus(SecureOrderStatus.CANCELLED, Instant.now());
    }
}
