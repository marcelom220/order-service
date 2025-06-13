package br.com.itau.secure.api.model;

import java.time.Instant;

public record OccurrenceDTO(
        String id,
        long productId,
        String type,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}