package br.com.itau.secure.api.mapper;

import br.com.itau.secure.api.model.SecureOrderInput;
import br.com.itau.secure.domain.model.SecureOrder;


public class SecureOrderMapper {

    public static SecureOrder toEntity(SecureOrderInput input) {
        return SecureOrder.builder()
                .customerId(input.getCustomerId())
                .productId(input.getProductId())
                .category(input.getCategory())
                .salesChannel(input.getSalesChannel())
                .paymentMethod(input.getPaymentMethod())
                .totalMonthlyPremiumAmount(input.getTotalMonthlyPremiumAmount())
                .insuredAmount(input.getInsuredAmount())
                .coverages(input.getCoverages())
                .assistances(input.getAssistances())
                .build();
    }
}