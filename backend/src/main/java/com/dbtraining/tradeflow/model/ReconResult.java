package com.dbtraining.tradeflow.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * ============================================================================
 * ReconResult — TICKET-I024 + TICKET-I058
 * ============================================================================
 * WHAT:    Outcome of comparing one trade against its external counterpart.
 *          One row per break (or per matched trade, depending on team policy).
 * HOW:     POJO on Day 2; @Entity on Day 5.
 * WHY:     The Ops UI page on Day 8 lists ReconResults so users can resolve.
 * OBSERVE: A row with status='OPEN' and discrepancyType=PRICE_MISMATCH means
 *          a human has to investigate.
 * ============================================================================
 *  TODO(TICKET-I024) [Day 2]:
 *    Fields: id, tradeId (Long), status (String for now), discrepancyType
 *            (DiscrepancyType, nullable), resolvedAt (Instant, nullable),
 *            createdAt (Instant).
 *
 *  TODO(TICKET-I058) [Day 5]:
 *    Convert to JPA entity.
 *    - @ManyToOne(fetch = LAZY) on the Trade reference
 *    - @Enumerated(EnumType.STRING) on discrepancyType
 *    - resolvedAt is @Column(nullable = true)
 * ============================================================================
 */
@Entity
@Table(name = "recon_breaks")
public class ReconResult {

    public enum Status { OPEN, RESOLVED, SUPPRESSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_id")
    private Trade trade;

    @Enumerated(EnumType.STRING)
    @Column(name = "discrepancy_type", nullable = false, length = 30)
    private DiscrepancyType discrepancyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    /** Some discrepancy in tickets about the table column being called created_at
        and the corresponding Java field being called detectedAt, but it should not 
        cause issues. Keeping this naming convention to avoid breaking other code */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected ReconResult() {}

    private ReconResult(Builder builder) {
        this.trade = builder.trade;
        this.status = builder.status != null ? builder.status : Status.OPEN;
        this.discrepancyType = builder.discrepancyType;
        this.detectedAt = builder.detectedAt != null ? builder.detectedAt : Instant.now();
        this.resolvedAt = builder.resolvedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public Trade getTrade() {
        return trade;
    }

    public Status getStatus() {
        return status;
    }

    public DiscrepancyType getDiscrepancyType() {
        return discrepancyType;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void resolve() {
        if (status == Status.RESOLVED) {
            return;
        }

        status = Status.RESOLVED;
        resolvedAt = Instant.now();
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    @Override
    public String toString() {
        return "ReconResult[trade=" + trade + " | " + discrepancyType + " | " + status + "]";
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ReconResult other)) {
            return false;
        }

        return Objects.equals(trade, other.trade)
                && discrepancyType == other.discrepancyType
                && Objects.equals(detectedAt, other.detectedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trade, discrepancyType, detectedAt);
    }

    public static final class Builder {

        private Trade trade;
        private Status status;
        private DiscrepancyType discrepancyType;
        private Instant detectedAt;
        private Instant resolvedAt;

        public Builder trade(Trade value) {
            this.trade = value;
            return this;
        }

        public Builder status(Status value) {
            this.status = value;
            return this;
        }

        public Builder discrepancyType(DiscrepancyType value) {
            this.discrepancyType = value;
            return this;
        }

        public Builder detectedAt(Instant value) {
            this.detectedAt = value;
            return this;
        }

        public Builder resolvedAt(Instant value) {
            this.resolvedAt = value;
            return this;
        }

        public ReconResult build() {
            Objects.requireNonNull(trade, "trade required");
            Objects.requireNonNull(discrepancyType, "discrepancyType required");
            return new ReconResult(this);
        }
    }
}
