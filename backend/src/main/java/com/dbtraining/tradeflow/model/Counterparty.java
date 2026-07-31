package com.dbtraining.tradeflow.model;

import jakarta.persistence.*;
import java.util.Objects;


/**
 * ============================================================================
 * Counterparty — TICKET-I022 + TICKET-I057
 * ============================================================================
 * WHAT:    The other side of a trade — broker, exchange, or another bank.
 * HOW:     Plain POJO on Day 2; converted to JPA @Entity on Day 5.
 * WHY:     The trade table has an FK to this — every trade has a counterparty.
 * OBSERVE: Build a Counterparty in main(), pass it through a Trade, print both.
 * ============================================================================
 *  TODO(TICKET-I022) [Day 2]:
 *    Fields: id (Long), name (String), leiCode (String, 20 chars), region (String).
 *    Add private constructor + Builder for clean construction.
 *    Add equals()/hashCode() on leiCode (it's globally unique).
 *
 *  TODO(TICKET-I057) [Day 5]:
 *    Convert this class to a JPA entity.
 *    - @Entity @Table(name = "counterparties")
 *    - @Id @GeneratedValue(strategy = GenerationType.IDENTITY) on id
 *    - @Column(unique = true, length = 20) on leiCode
 *    - protected no-arg constructor (JPA needs it).
 * ============================================================================
 */
// backend/src/main/java/com/dbtraining/tradeflow/model/Counterparty.java
@Entity
@Table(name = "counterparties")
public class Counterparty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(
            name = "lei_code",
            nullable = false,
            unique = true,
            length = 20
    )
    private String leiCode;

    @Column(nullable = false, length = 10)
    private String region;

    protected Counterparty() {}

    private Counterparty(Builder builder) {
        this.name = builder.name;
        this.leiCode = builder.leiCode;
        this.region = builder.region;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLeiCode() {
        return leiCode;
    }

    public String getRegion() {
        return region;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Counterparty other)) {
            return false;
        }

        return Objects.equals(leiCode, other.leiCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leiCode);
    }

    @Override
    public String toString() {
        return "Counterparty[" + leiCode + " | " + name + " | " + region + "]";
    }

    public static final class Builder {

        private String name;
        private String leiCode;
        private String region;

        public Builder name(String value) {
            this.name = value;
            return this;
        }

        public Builder leiCode(String value) {
            this.leiCode = value;
            return this;
        }

        public Builder region(String value) {
            this.region = value;
            return this;
        }

        public Counterparty build() {
            Objects.requireNonNull(name, "name required");
            Objects.requireNonNull(leiCode, "leiCode required");
            Objects.requireNonNull(region, "region required");

            if (leiCode.length() != 20) {
                throw new IllegalStateException(
                        "leiCode must be exactly 20 chars (LEI standard)"
                );
            }

            if (!region.matches("APAC|EMEA|NAMR|LATAM")) {
                throw new IllegalStateException(
                        "region must be one of APAC|EMEA|NAMR|LATAM"
                );
            }

            return new Counterparty(this);
        }
    }
}
