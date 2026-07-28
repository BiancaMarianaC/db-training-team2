package com.dbtraining.tradeflow.dto;

import com.dbtraining.tradeflow.model.DiscrepancyType;

import java.util.List;

/** A reconciliation break and all reasons identified for its trade. */
public record Discrepancy(String tradeRef, List<DiscrepancyType> types) {
}
