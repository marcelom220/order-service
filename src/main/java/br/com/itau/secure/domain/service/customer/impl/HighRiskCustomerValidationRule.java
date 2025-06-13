package br.com.itau.secure.domain.service.customer.impl;

import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.service.customer.CustomerTypeValidationRule;
import br.com.itau.secure.domain.service.status.InsuranceCategory;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class HighRiskCustomerValidationRule implements CustomerTypeValidationRule {


    // Limites para Cliente Alto Risco
    private static final BigDecimal AUTO_LIMIT = new BigDecimal("250000.00"); // Assumindo R$ 250.000,00
    private static final BigDecimal HOME_LIMIT = new BigDecimal("150000.00"); // Assumindo R$ 150.000,00
    private static final BigDecimal OTHER_LIMIT = new BigDecimal("125000.00"); // Para qualquer outro, incluindo VIDA se não especificado

    @Override
    public boolean checkRules(SecureOrder secureOrder, StringBuilder rejectionReason) {
        InsuranceCategory category = InsuranceCategory.fromString(secureOrder.getCategory());
        BigDecimal insuredAmount = secureOrder.getInsuredAmount();

        log.debug("Applying HighRiskCustomer rules for order: {}, Category: {}, Amount: {}",
                secureOrder.getId(), category, insuredAmount);
        if (insuredAmount == null) {
            rejectionReason.append("Insured amount not specified for High Risk customer.");
            log.warn("Order {}: {}", secureOrder.getId(), rejectionReason.toString());
            return false;
        }

        String specificReason = "";
        boolean passes = true;

        switch (category) {
            case AUTO:
                if (insuredAmount.compareTo(AUTO_LIMIT) > 0) {
                    specificReason = "Insured amount for Auto (R$" + insuredAmount + ") exceeds limit R$" + AUTO_LIMIT;
                    passes = false;
                }
                break;
            case HOME:
                if (insuredAmount.compareTo(HOME_LIMIT) > 0) {
                    specificReason = "Insured amount for Home (R$" + insuredAmount + ") exceeds limit R$" + HOME_LIMIT;
                    passes = false;
                }
                break;
            case LIFE: // Vida se enquadra na regra de "qualquer outro tipo de seguro" para Alto Risco
            case OTHER:
            default:
                if (insuredAmount.compareTo(OTHER_LIMIT) > 0) {
                    specificReason = "Insured amount for " + category.getDisplayName() +
                            " (R$" + insuredAmount + ") exceeds limit R$" + OTHER_LIMIT +
                            " for High Risk profile.";
                    passes = false;
                }
                break;
        }

        if (!passes) {
            rejectionReason.append("High Risk customer rule violation: ").append(specificReason);
            log.warn("Order {}: {}", secureOrder.getId(), rejectionReason.toString());
        }
        return passes;
    }
}
