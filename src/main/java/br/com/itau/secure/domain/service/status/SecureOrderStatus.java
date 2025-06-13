package br.com.itau.secure.domain.service.status;

import br.com.itau.secure.api.model.FraudCheckResult;
import br.com.itau.secure.api.model.PaymentConfirmation;
import br.com.itau.secure.api.model.SubscriptionAuthorization;
import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.service.status.impl.PendingStateStrategy;
import br.com.itau.secure.domain.service.status.impl.ReceivedStateStrategy;
import br.com.itau.secure.domain.service.status.impl.TerminalStateStrategy;
import br.com.itau.secure.domain.service.status.impl.ValidatedStateStrategy;

public enum SecureOrderStatus {
    RECEIVED(new ReceivedStateStrategy()),
    VALIDATED(new ValidatedStateStrategy()),
    PENDING(new PendingStateStrategy()),
    REJECTED(new TerminalStateStrategy("REJECTED")), // Example of a generic terminal strategy
    APPROVED(new TerminalStateStrategy("APPROVED") {
        @Override
        public void moveToCancel(SecureOrder secureOrder) {
            throw new UnsupportedOperationException("PolicyRequest is APPROVED and cannot be cancelled.");
        }
    }),
    CANCELLED(new TerminalStateStrategy("CANCELLED"));

    private final SecureOrderStateStrategy strategy;

    SecureOrderStatus(SecureOrderStateStrategy strategy) {
        this.strategy = strategy;
    }

    public void moveToValidate(SecureOrder secureOrder, FraudCheckResult fraudCheckResult) {
        this.strategy.moveToValidate(secureOrder, fraudCheckResult);
    }

    public void  moveToPending(FraudCheckResult fraudCheckResult, SecureOrder secureOrder) {
        this.strategy.moveToPending(fraudCheckResult, secureOrder);
    }

    public void  moveToApprove(SecureOrder secureOrder, PaymentConfirmation payment, SubscriptionAuthorization subscription) {
        this.strategy.moveToApprove(secureOrder, payment, subscription);
    }

    public void  moveToReject(SecureOrder secureOrder) {
        this.strategy.moveToReject(secureOrder);
    }

    public void  moveToCancel(SecureOrder secureOrder) {
        this.strategy.moveToCancel(secureOrder);
    }
}
