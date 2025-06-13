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
public class PendingStateStrategy implements SecureOrderStateStrategy {
    @Override
    public void moveToValidate(SecureOrder secureOrder, FraudCheckResult fraudCheckResult) {
        throw new UnsupportedOperationException("Cannot re-validate from PENDING state in this manner. Consider re-processing logic.");
    }

    @Override
    public void moveToPending(FraudCheckResult fraudCheckResult, SecureOrder secureOrder) {
        throw new UnsupportedOperationException("Cannot re-validate from PENDING state in PEDING;");

    }
    @Override
    public void moveToApprove(SecureOrder request, PaymentConfirmation payment, SubscriptionAuthorization subscription) {

        if (payment.isConfirmed() && subscription.isAuthorized()) {
            log.info("PolicyRequest " + request.getId() + " approved from PENDING state.");
            request.setStatus(SecureOrderStatus.APPROVED, Instant.now());
        } else {

            if (!payment.isConfirmed()) {
                request.setStatus(SecureOrderStatus.REJECTED, Instant.now());
            }
            if (!subscription.isAuthorized()) {
                request.setStatus(SecureOrderStatus.REJECTED, Instant.now());
            }
        }
    }

    @Override
    public void moveToReject(SecureOrder secureOrder) {
        log.info("PolicyRequest " + secureOrder.getId() + " rejected from PENDING state");
        secureOrder.setStatus(SecureOrderStatus.REJECTED, Instant.now());
    }

    @Override
    public void moveToCancel(SecureOrder secureOrder) {
        log.info("PolicyRequest " + secureOrder.getId() + " cancelled from PENDING state.");
        secureOrder.setStatus(SecureOrderStatus.CANCELLED, Instant.now());
    }


}
