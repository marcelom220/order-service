package br.com.itau.secure.domain.service.customer.impl;

import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.service.customer.CustomerTypeValidationRule;
import br.com.itau.secure.domain.service.status.InsuranceCategory;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class NoInformationCustomerValidationRule implements CustomerTypeValidationRule {

    // Limites para Cliente Sem Informação
    private static final BigDecimal LIFE_HOME_LIMIT = new BigDecimal("200000.00");
    private static final BigDecimal AUTO_LIMIT = new BigDecimal("75000.00");
    private static final BigDecimal OTHER_LIMIT = new BigDecimal("55000.00");

    @Override
    public boolean checkRules(SecureOrder secureOrder, StringBuilder rejectionReason) {
        InsuranceCategory category = InsuranceCategory.fromString(secureOrder.getCategory());
    BigDecimal insuredAmount = secureOrder.getInsuredAmount();

        log.debug("Applying NoInformationCustomer rules for order: {}, Category: {}, Amount: {}",
                secureOrder.getId(),category,insuredAmount);

        if(insuredAmount ==null)

    {
        rejectionReason.append("Insured amount not specified for No Information customer.");
        log.warn("Order {}: {}", secureOrder.getId(), rejectionReason.toString());
        return false;
    }

    String specificReason = "";
    boolean passes = true;

        switch(category)

    {
        case LIFE:
        case HOME:
            if (insuredAmount.compareTo(LIFE_HOME_LIMIT) > 0) {
                specificReason = "Insured amount for " + category.getDisplayName() +
                        " (R$" + insuredAmount + ") exceeds limit R$" + LIFE_HOME_LIMIT;
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

        if(!passes)

    {
        rejectionReason.append("No Information customer rule violation: ").append(specificReason);
        log.warn("Order {}: {}", secureOrder.getId(), rejectionReason.toString());
    }
        return passes;
}
}
