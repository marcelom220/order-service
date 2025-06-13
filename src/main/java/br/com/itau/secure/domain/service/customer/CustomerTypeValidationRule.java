package br.com.itau.secure.domain.service.customer;

import br.com.itau.secure.domain.model.SecureOrder;

public interface CustomerTypeValidationRule {
    boolean checkRules(SecureOrder secureOrder, StringBuilder rejectionReason);
}
