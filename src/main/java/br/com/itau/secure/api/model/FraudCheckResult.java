package br.com.itau.secure.api.model;


import java.time.Instant;
import java.util.List;

public record FraudCheckResult(
        String orderId,
        String customerId,
        Instant analyzedAt,
        String classification,
        List<OccurrenceDTO> occurrences
) {}

