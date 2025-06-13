package br.com.itau.secure.domain.service.customer.impl;

import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.service.customer.CustomerTypeValidationRule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultCustomerValidationRule implements CustomerTypeValidationRule {
    @Override
    public boolean checkRules(SecureOrder secureOrder, StringBuilder rejectionReason) {
        String reason = "Customer risk profile is UNKNOWN or not handled. Order cannot be processed automatically.";
        rejectionReason.append(reason);
        log.warn("Order {}: {}", secureOrder.getId(), reason);

        return false;
    }
}
