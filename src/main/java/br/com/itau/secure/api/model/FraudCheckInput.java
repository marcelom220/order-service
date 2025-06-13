package br.com.itau.secure.api.model;

import lombok.Builder;

@Builder
public record FraudCheckInput(String orderId, String customerId) {}
