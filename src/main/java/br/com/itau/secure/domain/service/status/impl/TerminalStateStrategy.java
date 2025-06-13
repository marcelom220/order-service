package br.com.itau.secure.domain.service.status.impl;

import br.com.itau.secure.api.model.FraudCheckResult;
import br.com.itau.secure.api.model.PaymentConfirmation;
import br.com.itau.secure.api.model.SubscriptionAuthorization;
import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.service.status.SecureOrderStateStrategy;

public class TerminalStateStrategy implements SecureOrderStateStrategy {
    private final String stateName;

    public TerminalStateStrategy(String stateName) {
        this.stateName = stateName;
    }

    private void throwTerminalStateEx() {
        throw new UnsupportedOperationException("PolicyRequest is in a terminal state: " + stateName + " and cannot be modified.");
    }
    @Override public void moveToValidate(SecureOrder secureOrder, FraudCheckResult fraudCheckResult) { throwTerminalStateEx(); }
    @Override public void moveToPending(FraudCheckResult fraudCheckResult, SecureOrder secureOrder) { throwTerminalStateEx(); }
    @Override public void moveToApprove(SecureOrder secureOrder, PaymentConfirmation payment, SubscriptionAuthorization subscription) { throwTerminalStateEx(); }
    @Override public void moveToReject(SecureOrder secureOrder) { throwTerminalStateEx(); }
    @Override public void moveToCancel(SecureOrder secureOrder) { throwTerminalStateEx(); }
}
