package br.com.itau.secure.domain.model;

import br.com.itau.secure.api.model.FraudCheckInput;
import br.com.itau.secure.domain.service.status.SecureOrderStatus;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import static br.com.itau.secure.commom.IdGenerator.generateTimeBasedUUID;

@Document(collection = "secure_orders")
@Getter
public class SecureOrder {

    @Id
    private String id;
    private String customerId;
    private String productId;
    private String category;
    private String salesChannel;
    private String paymentMethod;
    private SecureOrderStatus status;
    private Instant createdAt;
    private Instant finishedAt;
    private BigDecimal totalMonthlyPremiumAmount;
    private BigDecimal insuredAmount;
    private Map<String, BigDecimal> coverages;
    private List<String> assistances;
    private List<History> history;

    @Builder
    public SecureOrder(String customerId, String productId, String category, String salesChannel,
                       String paymentMethod, Instant finishedAt, BigDecimal totalMonthlyPremiumAmount,
                       BigDecimal insuredAmount, Map<String, BigDecimal> coverages, List<String> assistances,
                       List<History> history) {
        this.id = generateTimeBasedUUID().toString();
        this.customerId = customerId;
        this.productId = productId;
        this.category = category;
        this.salesChannel = salesChannel;
        this.paymentMethod = paymentMethod;
        this.createdAt = Instant.now();
        this.finishedAt = finishedAt;
        this.totalMonthlyPremiumAmount = totalMonthlyPremiumAmount;
        this.insuredAmount = insuredAmount;
        this.coverages = coverages;
        this.assistances = assistances;
        this.history = history;

        if(this.history == null) {
            this.history = new ArrayList<>();
            setStatus(SecureOrderStatus.RECEIVED, this.createdAt);
        }
    }

    public void setStatus(SecureOrderStatus newStatus, Instant data) {
        this.history.add(new History(newStatus, data != null? data: Instant.now()));
        this.status = newStatus;
    }

    @Data
    @Builder
    public static class History {
        private SecureOrderStatus status;
        private Instant timestamp;
    }

    public FraudCheckInput toFraudCheckInput() {
        return FraudCheckInput.builder()
                .orderId(this.getId())
                .customerId(this.customerId).build();
    }

}
