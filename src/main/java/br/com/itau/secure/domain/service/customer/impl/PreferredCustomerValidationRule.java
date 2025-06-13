package br.com.itau.secure.domain.service.customer.impl;

import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.service.customer.CustomerTypeValidationRule;
import br.com.itau.secure.domain.service.status.InsuranceCategory;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class PreferredCustomerValidationRule implements CustomerTypeValidationRule {


    // Limites para Cliente Preferencial
    private static final BigDecimal LIFE_LIMIT = new BigDecimal("800000.00");
    private static final BigDecimal AUTO_HOME_LIMIT = new BigDecimal("450000.00");
    private static final BigDecimal OTHER_LIMIT = new BigDecimal("375000.00");

    @Override
    public boolean checkRules(SecureOrder secureOrder, StringBuilder rejectionReason) {
        InsuranceCategory category = InsuranceCategory.fromString(secureOrder.getCategory());
        BigDecimal insuredAmount = secureOrder.getInsuredAmount();

        log.debug("Applying PreferredCustomer rules for order: {}, Category: {}, Amount: {}",
                secureOrder.getId(), category, insuredAmount);
        if (insuredAmount == null) {
            rejectionReason.append("Insured amount not specified for Preferred customer.");
            log.warn("Order {}: {}", secureOrder.getId(), rejectionReason.toString());
            return false;
        }

        String specificReason = "";
        boolean passes = true;

        switch (category) {
            case LIFE:
                // "Capital segurado inferior a R$ 800.000,00"
                if (insuredAmount.compareTo(LIFE_LIMIT) >= 0) {
                    specificReason = "Insured amount for Life (R$" + insuredAmount + ") is not less than limit R$" + LIFE_LIMIT;
                    passes = false;
                }
                break;
            case AUTO:
            case HOME:
                // "Capital segurado inferior a R$ 450.000,00"
                if (insuredAmount.compareTo(AUTO_HOME_LIMIT) >= 0) {
                    specificReason = "Insured amount for " + category.getDisplayName() + " (R$" + insuredAmount + ") is not less than limit R$" + AUTO_HOME_LIMIT;
                    passes = false;
                }
                break;
            case OTHER:
            default: // Inclui OTHER e qualquer categoria não mapeada explicitamente
                // "Capital segurado não ultrapasse R$ 375.000,00" ( <= )
                if (insuredAmount.compareTo(OTHER_LIMIT) > 0) {
                    specificReason = "Insured amount for Other category (R$" + insuredAmount + ") exceeds limit R$" + OTHER_LIMIT;
                    passes = false;
                }
                break;
        }

        if (!passes) {
            rejectionReason.append("Preferred customer rule violation: ").append(specificReason);
            log.warn("Order {}: {}", secureOrder.getId(), rejectionReason.toString());
        }
        return passes;
    }
}
