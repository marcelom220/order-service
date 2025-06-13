package br.com.itau.secure.domain.service.customer;

import br.com.itau.secure.domain.service.customer.impl.*;

import java.util.EnumMap;
import java.util.Map;

public class CustomerTypeValidationRuleFactory {
    private final Map<CustomerRiskProfile, CustomerTypeValidationRule> strategies;

    public CustomerTypeValidationRuleFactory() {
        strategies = new EnumMap<>(CustomerRiskProfile.class);
        strategies.put(CustomerRiskProfile.REGULAR, new RegularCustomerValidationRule());
        strategies.put(CustomerRiskProfile.HIGH_RISK, new HighRiskCustomerValidationRule()); // Implemente esta
        strategies.put(CustomerRiskProfile.PREFERRED, new PreferredCustomerValidationRule()); // Implemente esta
        strategies.put(CustomerRiskProfile.NO_INFORMATION, new NoInformationCustomerValidationRule()); // Implemente esta
        strategies.put(CustomerRiskProfile.UNKNOWN, new DefaultCustomerValidationRule()); // Implemente esta (pode rejeitar por padrão)
    }

    public CustomerTypeValidationRule getStrategy(CustomerRiskProfile profile) {
        return strategies.getOrDefault(profile, strategies.get(CustomerRiskProfile.UNKNOWN));
    }
}
