package com.dbtraining.tradeflow.model;

import java.util.Objects;


/**
 * ============================================================================
 * Instrument — TICKET-I023 + TICKET-I057
 * ============================================================================
 * WHAT:    What is being traded — equity, bond, FX pair, future, etc.
 * HOW:     POJO on Day 2; @Entity on Day 5.
 * WHY:     The trade table has an FK to this; the dashboard groups trades by
 *          instrument.
 * OBSERVE: Set assetClass = EQUITY, currency = "EUR", and the trade summary
 *          should treat it as a cash product.
 * ============================================================================
 *  TODO(TICKET-I023) [Day 2]:
 *    Fields: id (Long), symbol (String, e.g. "SAP.DE"), name (String),
 *            assetClass (AssetClass), currency (String length 3).
 *    Validate currency length in the Builder.
 *
 *  TODO(TICKET-I057) [Day 5]:
 *    Make this a JPA entity.
 *    - @Enumerated(EnumType.STRING) on assetClass
 *    - @Column(length = 3, nullable = false) on currency
 * ============================================================================
 */
// backend/src/main/java/com/dbtraining/tradeflow/model/Instrument.java
public class Instrument {

    private Long id;
    private String symbol;
    private String name;
    private AssetClass assetClass;
    private String currency;
    private String isin;

    Instrument() {}

    private Instrument(Builder b) {
        this.symbol     = b.symbol;
        this.name       = b.name;
        this.assetClass = b.assetClass;
        this.currency   = b.currency;
        this.isin       = b.isin;
    }

    public static Builder builder() { return new Builder(); }

    public Long getId()              { return id; }
    public String getSymbol()        { return symbol; }
    public String getName()          { return name; }
    public AssetClass getAssetClass(){ return assetClass; }
    public String getCurrency()      { return currency; }
    public String getIsin()          { return isin; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Instrument other)) return false;
        return Objects.equals(symbol, other.symbol);
    }

    @Override public int hashCode() { return Objects.hash(symbol); }

    @Override public String toString() {
        return "Instrument[" + symbol + " | " + name + " | " + assetClass + " " + currency + "]";
    }

    public static final class Builder {
        private String symbol;
        private String name;
        private AssetClass assetClass;
        private String currency;
        private String isin;

        public Builder symbol(String v)         { this.symbol = v;     return this; }
        public Builder name(String v)           { this.name = v;       return this; }
        public Builder assetClass(AssetClass v) { this.assetClass = v; return this; }
        public Builder currency(String v)       { this.currency = v == null ? null : v.toUpperCase(); return this; }
        public Builder isin(String v)           { this.isin = v;       return this; }

        public Instrument build() {
            Objects.requireNonNull(symbol,     "symbol required");
            Objects.requireNonNull(name,       "name required");
            Objects.requireNonNull(assetClass, "assetClass required");
            Objects.requireNonNull(currency,   "currency required");
            if (currency.length() != 3)
                throw new IllegalStateException("currency must be ISO-4217 3-letter code");
            if (isin != null && isin.length() != 12)
                throw new IllegalStateException("ISIN must be exactly 12 chars when set");
            return new Instrument(this);
        }
    }
}
