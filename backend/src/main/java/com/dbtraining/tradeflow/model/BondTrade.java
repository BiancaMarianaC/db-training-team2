package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * ============================================================================
 * BondTrade — TICKET-I031
 * ============================================================================
 * WHAT:    A trade on a fixed-income instrument.
 * WHY:     Bonds need coupon and maturity to compute yield / settlement.
 * ============================================================================
 *  TODO(TICKET-I031):
 *    extends BaseTrade.
 *    Extra fields:
 *      private final BigDecimal couponRate;       // 0.0 .. 100.0
 *      private final LocalDate maturityDate;      // must be after tradeDate
 *      private final BigDecimal faceValue;
 *
 *    Validate in builder:
 *      - couponRate >= 0 && couponRate <= 100
 *      - maturityDate.isAfter(tradeDate)
 *
 *    assetClassDescription() returns
 *      "Bond coupon " + couponRate + "% mat " + maturityDate.
 * ============================================================================
 */
public class BondTrade extends BaseTrade {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BigDecimal couponRate;
    private final LocalDate maturityDate;
    private final BigDecimal faceValue;

    private BondTrade(Builder builder) {
        super(builder.tradeRef, builder.instrumentId, builder.counterpartyId,
                builder.quantity, builder.price, builder.tradeDate,
                builder.status, builder.createdAt);
        this.couponRate = Objects.requireNonNull(builder.couponRate, "couponRate required");
        this.maturityDate = Objects.requireNonNull(builder.maturityDate, "maturityDate required");
        this.faceValue = Objects.requireNonNull(builder.faceValue, "faceValue required");

        if (couponRate.signum() < 0 || couponRate.compareTo(HUNDRED) > 0) {
            throw new IllegalStateException("couponRate must be between 0 and 100");
        }
        if (!maturityDate.isAfter(tradeDate)) {
            throw new IllegalStateException("maturityDate must be after tradeDate");
        }
        if (faceValue.signum() <= 0) {
            throw new IllegalStateException("faceValue must be > 0");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public BigDecimal getCouponRate() {
        return couponRate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public BigDecimal getFaceValue() {
        return faceValue;
    }

    @Override
    public String assetClassDescription() {
        return "Bond coupon " + couponRate + "% mat " + maturityDate;
    }

    public static final class Builder {

        private String tradeRef;
        private Long instrumentId;
        private Long counterpartyId;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDate tradeDate;
        private TradeStatus status;
        private Instant createdAt;
        private BigDecimal couponRate;
        private LocalDate maturityDate;
        private BigDecimal faceValue;

        public Builder tradeRef(String value) {
            this.tradeRef = value;
            return this;
        }

        public Builder instrumentId(Long value) {
            this.instrumentId = value;
            return this;
        }

        public Builder counterpartyId(Long value) {
            this.counterpartyId = value;
            return this;
        }

        public Builder quantity(BigDecimal value) {
            this.quantity = value;
            return this;
        }

        public Builder price(BigDecimal value) {
            this.price = value;
            return this;
        }

        public Builder tradeDate(LocalDate value) {
            this.tradeDate = value;
            return this;
        }

        public Builder status(TradeStatus value) {
            this.status = value;
            return this;
        }

        public Builder createdAt(Instant value) {
            this.createdAt = value;
            return this;
        }

        public Builder couponRate(BigDecimal value) {
            this.couponRate = value;
            return this;
        }

        public Builder maturityDate(LocalDate value) {
            this.maturityDate = value;
            return this;
        }

        public Builder faceValue(BigDecimal value) {
            this.faceValue = value;
            return this;
        }

        public BondTrade build() {
            return new BondTrade(this);
        }
    }
}
