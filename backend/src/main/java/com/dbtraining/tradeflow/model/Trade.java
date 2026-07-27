package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * ============================================================================
 * Trade — TICKET-I017 + TICKET-I018 + TICKET-I025 + TICKET-I056
 * ============================================================================
 * WHAT:    Domain object representing a single trade. Central to the system.
 * HOW:     Plain POJO with private final fields and a fluent Builder.
 *          On Day 5 we convert it to a JPA @Entity.
 * WHY:     Immutability + Builder = thread-safe construction + a readable
 *          API at call sites. JPA needs a no-arg constructor — keep it
 *          protected so the Builder is still the only public way in.
 * OBSERVE: Trade t = Trade.builder().tradeRef("TRD-1").quantity(...).build();
 *          Two trades with the same tradeRef should be .equals().
 * ============================================================================
 *  TICKET-I017: define the fields and getters.
 *  TICKET-I018: add the Builder.
 *  TICKET-I025: override equals()/hashCode() using ONLY tradeRef.
 *  TICKET-I056: add JPA annotations — @Entity / @Table / @Id / @ManyToOne.
 * ============================================================================
 *
 * HINTS:
 * - Use BigDecimal for `quantity` + `price` (NEVER double — it loses precision
 *   for money).
 * - Use LocalDate (NOT Date) for tradeDate.
 * - Use Instant (NOT Date) for createdAt.
 * - For JPA: a `protected Trade()` no-arg constructor satisfies Hibernate;
 *   the public path stays via the Builder.
 * - For @ManyToOne on instrument/counterparty: use FetchType.LAZY to avoid
 *   accidental N+1 queries.
 * ============================================================================
 */
public class Trade {

    private final String tradeRef;
    private final Long instrumentId;
    private final Long counterpartyId;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private final LocalDate tradeDate;
    private final TradeStatus status;
    private final Instant createdAt;

    private Trade(String tradeRef, Long instrumentId, Long counterpartyId,
                  BigDecimal quantity, BigDecimal price, LocalDate tradeDate,
                  TradeStatus status, Instant createdAt) {
        this.tradeRef = tradeRef;
        this.instrumentId = instrumentId;
        this.counterpartyId = counterpartyId;
        this.quantity = quantity;
        this.price = price;
        this.tradeDate = tradeDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Protected no-arg constructor — satisfies JPA/Hibernate (TICKET-I056).
    // The Builder (TICKET-I018) remains the only public construction path.
    protected Trade() {
        this(null, null, null, null, null, null, null, null);
    }

    public String getTradeRef()         { return tradeRef; }
    public Long getInstrumentId()       { return instrumentId; }
    public Long getCounterpartyId()     { return counterpartyId; }
    public BigDecimal getQuantity()     { return quantity; }
    public BigDecimal getPrice()        { return price; }
    public LocalDate getTradeDate()     { return tradeDate; }
    public TradeStatus getStatus()      { return status; }
    public Instant getCreatedAt()       { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trade other)) return false;
        return Objects.equals(tradeRef, other.tradeRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tradeRef);
    }

    @Override
    public String toString() {
        return "Trade[" + tradeRef + " | instrument=" + instrumentId
                + " | " + quantity + " @ " + price
                + " | " + tradeDate + " | " + status + "]";
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tradeRef;
        private Long instrumentId;
        private Long counterpartyId;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDate tradeDate;
        private TradeStatus status;
        private Instant createdAt;

        public Builder tradeRef(String v)        { this.tradeRef = v;       return this; }
        public Builder instrumentId(Long v)      { this.instrumentId = v;   return this; }
        public Builder counterpartyId(Long v)    { this.counterpartyId = v; return this; }
        public Builder quantity(BigDecimal v)    { this.quantity = v;       return this; }
        public Builder price(BigDecimal v)       { this.price = v;          return this; }
        public Builder tradeDate(LocalDate v)    { this.tradeDate = v;      return this; }
        public Builder status(TradeStatus v)     { this.status = v;         return this; }
        public Builder createdAt(Instant v)      { this.createdAt = v;      return this; }

        public Trade build() {
            Objects.requireNonNull(tradeRef,       "tradeRef required");
            Objects.requireNonNull(instrumentId,   "instrumentId required");
            Objects.requireNonNull(counterpartyId, "counterpartyId required");
            Objects.requireNonNull(quantity,       "quantity required");
            Objects.requireNonNull(price,          "price required");
            Objects.requireNonNull(tradeDate,      "tradeDate required");
            if (quantity.signum() <= 0) throw new IllegalStateException("quantity must be > 0");
            if (price.signum() < 0)    throw new IllegalStateException("price must be >= 0");

            TradeStatus resolvedStatus = status != null ? status : TradeStatus.PENDING;
            Instant resolvedCreatedAt = createdAt != null ? createdAt : Instant.now();
            return new Trade(tradeRef, instrumentId, counterpartyId, quantity,
                    price, tradeDate, resolvedStatus, resolvedCreatedAt);
        }
    }
}
