package br.com.itau.secure.domain.service.status;

import br.com.itau.secure.api.model.FraudCheckResult;
import br.com.itau.secure.api.model.PaymentConfirmation;
import br.com.itau.secure.api.model.SubscriptionAuthorization;
import br.com.itau.secure.domain.model.SecureOrder;

public interface SecureOrderStateStrategy {
    void moveToValidate(SecureOrder secureOrder, FraudCheckResult fraudCheckResult);
    void moveToPending(FraudCheckResult fraudCheckResult, SecureOrder secureOrder);
    void moveToApprove(SecureOrder secureOrder, PaymentConfirmation payment, SubscriptionAuthorization subscription);
    void moveToReject(SecureOrder secureOrder);
    void moveToCancel(SecureOrder secureOrder);
}

