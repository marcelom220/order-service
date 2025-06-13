package br.com.itau.secure.api.model;

import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.service.status.SecureOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SecureOrderResponse(
        String id,
        String customerId,
        String productId,
        String category,
        String salesChannel,
        String paymentMethod,
        SecureOrderStatus status,
        Instant createdAt,
        Instant finishedAt,
        BigDecimal totalMonthlyPremiumAmount,
        BigDecimal insuredAmount,
        Map<String, BigDecimal> coverages,
        List<String> assistances,
        List<SecureOrder.History> history
) {
    public static SecureOrderResponse fromEntity(SecureOrder secureOrder) {
        return new SecureOrderResponse(
                secureOrder.getId(),
                secureOrder.getCustomerId(),
                secureOrder.getProductId(),
                secureOrder.getCategory(),
                secureOrder.getSalesChannel(),
                secureOrder.getPaymentMethod(),
                secureOrder.getStatus(),
                secureOrder.getCreatedAt(),
                secureOrder.getFinishedAt(),
                secureOrder.getTotalMonthlyPremiumAmount(),
                secureOrder.getInsuredAmount(),
                secureOrder.getCoverages(),
                secureOrder.getAssistances(),
                secureOrder.getHistory()
        );
    }
}
