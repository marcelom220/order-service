package br.com.itau.secure.domain.service.status.impl;

import br.com.itau.secure.api.model.FraudCheckResult;
import br.com.itau.secure.api.model.PaymentConfirmation;
import br.com.itau.secure.api.model.SubscriptionAuthorization;
import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.service.customer.CustomerRiskProfile;
import br.com.itau.secure.domain.service.customer.CustomerTypeValidationRule;
import br.com.itau.secure.domain.service.customer.CustomerTypeValidationRuleFactory;
import br.com.itau.secure.domain.service.status.SecureOrderStateStrategy;
import br.com.itau.secure.domain.service.status.SecureOrderStatus;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
public class ValidatedStateStrategy implements SecureOrderStateStrategy {

    private final CustomerTypeValidationRuleFactory ruleFactory;

    public ValidatedStateStrategy() {
        this.ruleFactory = new CustomerTypeValidationRuleFactory();
    }


    @Override
    public void moveToValidate(SecureOrder secureOrder, FraudCheckResult fraudCheckResult) {
        throw new UnsupportedOperationException("PolicyRequest is already VALIDATED.");
    }

    @Override
    public void moveToPending(FraudCheckResult fraudCheckResult, SecureOrder secureOrder) {
        log.info("Applying rules for order {} based on fraud check classification: {}",secureOrder.getId(), fraudCheckResult.classification());

        CustomerRiskProfile customerProfile = CustomerRiskProfile.fromString(fraudCheckResult.classification());

        if (customerProfile == CustomerRiskProfile.UNKNOWN) {
            log.warn("Order {} has an UNKNOWN risk profile from fraud check: {}. Rejecting.",
                    secureOrder.getId(), fraudCheckResult.classification());
            secureOrder.setStatus(SecureOrderStatus.REJECTED, Instant.now());
            return;
        }

        if (secureOrder.getInsuredAmount() == null) {
            log.error("InsuredAmount is null for order {}. Cannot apply rules. Rejecting.", secureOrder.getId());
            secureOrder.setStatus(SecureOrderStatus.REJECTED, Instant.now());
            return;
        }
        CustomerTypeValidationRule validationRule = ruleFactory.getStrategy(customerProfile);
        StringBuilder rejectionReason = new StringBuilder();
        boolean rulesPassed = validationRule.checkRules(secureOrder, rejectionReason);

        //  Define o status final
        if (rulesPassed) {
            log.info("Order {} passed additional validation rules. Moving to PENDING.", secureOrder.getId());
            secureOrder.setStatus(SecureOrderStatus.PENDING, Instant.now());
        } else {
            log.info("Order {} failed additional validation rules.",
                    secureOrder.getId());
            secureOrder.setStatus(SecureOrderStatus.REJECTED, Instant.now());
        }
    }

    @Override
    public void moveToApprove(SecureOrder secureOrder, PaymentConfirmation payment, SubscriptionAuthorization subscription) {
        throw new UnsupportedOperationException("Cannot approve from VALIDATED. Must go through PENDING.");
    }

    @Override
    public void moveToReject(SecureOrder secureOrder) {
        System.out.println("PolicyRequest " + secureOrder.getId() + " rejected from VALIDATED state.");
        secureOrder.setStatus(SecureOrderStatus.REJECTED, Instant.now());
    }

    @Override
    public void moveToCancel(SecureOrder secureOrder) {
        System.out.println("PolicyRequest " + secureOrder.getId() + " cancelled from VALIDATED state.");
        secureOrder.setStatus(SecureOrderStatus.CANCELLED, Instant.now());
    }

}
