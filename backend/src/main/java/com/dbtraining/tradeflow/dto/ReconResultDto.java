package com.dbtraining.tradeflow.dto;

import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;

import java.time.Instant;

public record ReconResultDto(
        Long id,
        Long tradeId,
        String tradeRef,
        Long counterpartyId,
        DiscrepancyType discrepancyType,
        ReconResult.Status status,
        Instant detectedAt,
        Instant resolvedAt
) {
    public static ReconResultDto from(ReconResult result) {
        return new ReconResultDto(
                result.getId(),
                result.getTrade().getId(),
                result.getTrade().getTradeRef(),
                result.getTrade().getCounterpartyId(),
                result.getDiscrepancyType(),
                result.getStatus(),
                result.getDetectedAt(),
                result.getResolvedAt()
        );
    }
}
