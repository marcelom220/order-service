package br.com.itau.secure.api.model;

import lombok.Builder;

@Builder
public record OrderServiceStatusData(String orderId, String status, FraudCheckResult fraudCheckResult) {

}
