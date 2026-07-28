package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * ============================================================================
 * FXTrade — TICKET-I030
 * ============================================================================
 * WHAT:    A foreign-exchange trade (EUR/USD, GBP/JPY, ...).
 * WHY:     FX has no exchange — pricing is OTC and settled bilaterally.
 *          We track the currency pair + spot rate explicitly.
 * ============================================================================
 *  TODO(TICKET-I030):
 *    extends BaseTrade.
 *    Extra fields:
 *      private final String baseCurrency;    // "EUR" in EUR/USD
 *      private final String quoteCurrency;   // "USD" in EUR/USD
 *      private final BigDecimal spotRate;
 *
 *    Validation in builder: baseCurrency != quoteCurrency.
 *    assetClassDescription() returns baseCurrency + "/" + quoteCurrency.
 * ============================================================================
 */
public class FXTrade extends BaseTrade {

    private final String baseCurrency;
    private final String quoteCurrency;
    private final BigDecimal spotRate;

    private FXTrade(Builder builder) {
        super(builder.tradeRef, builder.instrumentId, builder.counterpartyId,
                builder.quantity, builder.price, builder.tradeDate,
                builder.status, builder.createdAt);
        this.baseCurrency = Objects.requireNonNull(builder.baseCurrency, "baseCurrency required");
        this.quoteCurrency = Objects.requireNonNull(builder.quoteCurrency, "quoteCurrency required");
        this.spotRate = Objects.requireNonNull(builder.spotRate, "spotRate required");

        if (baseCurrency.length() != 3 || quoteCurrency.length() != 3) {
            throw new IllegalStateException("currencies must be ISO-4217 3-letter codes");
        }
        if (baseCurrency.equals(quoteCurrency)) {
            throw new IllegalStateException("base and quote currencies must differ");
        }
        if (spotRate.signum() <= 0) {
            throw new IllegalStateException("spotRate must be > 0");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public BigDecimal getSpotRate() {
        return spotRate;
    }

    @Override
    public String assetClassDescription() {
        return baseCurrency + "/" + quoteCurrency;
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
        private String baseCurrency;
        private String quoteCurrency;
        private BigDecimal spotRate;

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

        public Builder baseCurrency(String value) {
            this.baseCurrency = value;
            return this;
        }

        public Builder quoteCurrency(String value) {
            this.quoteCurrency = value;
            return this;
        }

        public Builder spotRate(BigDecimal value) {
            this.spotRate = value;
            return this;
        }

        public FXTrade build() {
            return new FXTrade(this);
        }
    }
}
