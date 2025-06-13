package br.com.itau.secure.domain.service.customer.impl;


import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.service.customer.CustomerTypeValidationRule;

import br.com.itau.secure.domain.service.status.InsuranceCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class RegularCustomerValidationRule implements CustomerTypeValidationRule {
    private static final Logger log = LoggerFactory.getLogger(RegularCustomerValidationRule.class);

    private static final BigDecimal LIFE_HOME_LIMIT = new BigDecimal("500000.00");
    private static final BigDecimal AUTO_LIMIT = new BigDecimal("350000.00");
    private static final BigDecimal OTHER_LIMIT = new BigDecimal("255000.00");

    @Override
    public boolean checkRules(SecureOrder secureOrder, StringBuilder rejectionReason) {
        InsuranceCategory category = InsuranceCategory.fromString(secureOrder.getCategory());
        BigDecimal insuredAmount = secureOrder.getInsuredAmount();

        log.debug("Applying RegularCustomer rules for order: {}, Category: {}, Amount: {}",
                secureOrder.getId(), category, insuredAmount);

        if (insuredAmount == null) {
            rejectionReason.append("Insured amount not specified for Regular customer.");
            log.warn("Order {}: {}", secureOrder.getId(), rejectionReason.toString());
            return false;
        }

        String specificReason = "";
        boolean passes = true;

        switch (category) {
            case LIFE:
            case HOME:
                if (insuredAmount.compareTo(LIFE_HOME_LIMIT) > 0) {
                    specificReason = "Insured amount for Life/Home (R$" + insuredAmount + ") exceeds limit R$" + LIFE_HOME_LIMIT;
                    passes = false;
                }
                break;
            case AUTO:
                if (insuredAmount.compareTo(AUTO_LIMIT) > 0) {
                    specificReason = "Insured amount for Auto (R$" + insuredAmount + ") exceeds limit R$" + AUTO_LIMIT;
                    passes = false;
                }
                break;
            case OTHER:
            default: // Inclui OTHER e qualquer categoria não mapeada explicitamente
                if (insuredAmount.compareTo(OTHER_LIMIT) > 0) {
                    specificReason = "Insured amount for Other category (R$" + insuredAmount + ") exceeds limit R$" + OTHER_LIMIT;
                    passes = false;
                }
                break;
        }

        if (!passes) {
            rejectionReason.append("Regular customer rule violation: ").append(specificReason);
            log.warn("Order {}: {}", secureOrder.getId(), rejectionReason.toString());
        }
        return passes;
    }
}
