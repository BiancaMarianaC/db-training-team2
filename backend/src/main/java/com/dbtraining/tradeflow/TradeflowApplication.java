package com.dbtraining.tradeflow;

import com.dbtraining.tradeflow.model.Trade;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ============================================================================
 * TradeflowApplication — Spring Boot entry point
 * ============================================================================
 * WHAT:    The single annotated main() that bootstraps the whole service.
 * HOW:     `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration`
 *          + `@ComponentScan` — scans this package and below for beans.
 * WHY:     One starting point, predictable lifecycle, easy to launch from
 *          IDE or `./mvnw spring-boot:run`.
 * OBSERVE: Boot log includes "Started TradeflowApplication in X seconds".
 * ============================================================================
 *  Tickets that touch this file:
 *   - TICKET-I016 — package structure + boot main
 *   - TICKET-I026 — print formatted trade list (Day 2, BEFORE Spring boot wiring)
 *   - TICKET-I040 — wire up the full recon pipeline run in main (Day 3 sprint)
 *
 *  Note: I026 runs BEFORE we have Spring Boot — for Day 2 you'll use a plain
 *  `public static void main` without `@SpringBootApplication`. From Day 5
 *  onward, this becomes the Spring Boot entry-point as below.
 * ============================================================================
 */
@SpringBootApplication
public class TradeflowApplication {

    public static void main(String[] args) {
        printBanner();
        verifyTradeEqualsContract();
        SpringApplication.run(TradeflowApplication.class, args);
    }

    // TICKET-I025: manual assertion proving Trade.equals()/hashCode() use
    // only tradeRef — two trades sharing a tradeRef but differing in
    // quantity/price must still be .equals() and share a hashCode().
    private static void verifyTradeEqualsContract() {
        Trade a = Trade.builder()
                .tradeRef("TRD-1")
                .instrumentId(1L).counterpartyId(1L)
                .quantity(new BigDecimal("100")).price(new BigDecimal("50.00"))
                .tradeDate(LocalDate.now())
                .build();
        Trade b = Trade.builder()
                .tradeRef("TRD-1")
                .instrumentId(1L).counterpartyId(1L)
                .quantity(new BigDecimal("200")).price(new BigDecimal("99.99"))
                .tradeDate(LocalDate.now())
                .build();
        if (!a.equals(b)) throw new AssertionError("Trade.equals() broken");
        if (a.hashCode() != b.hashCode()) throw new AssertionError("Trade.hashCode() broken");
    }

    private static void printBanner() {
        // TODO(TICKET-I026): On Day 2 this main() is plain Java — replace the
        //   SpringApplication.run call above with your console trade-table
        //   printout, then revert/extend it on Day 5 when Spring Boot enters.
        System.out.println();
        System.out.println("  ████████ ██████   █████  ██████  ███████ ███████ ██       ██████  ██     ██");
        System.out.println("     ██    ██   ██ ██   ██ ██   ██ ██      ██      ██      ██    ██ ██     ██");
        System.out.println("     ██    ██████  ███████ ██   ██ █████   █████   ██      ██    ██ ██  █  ██");
        System.out.println("     ██    ██   ██ ██   ██ ██   ██ ██      ██      ██      ██    ██ ██ ███ ██");
        System.out.println("     ██    ██   ██ ██   ██ ██████  ███████ ██      ███████  ██████   ███ ███");
        System.out.println();
        System.out.println("  Deutsche Bank — TDI 2026 Graduate Technical Training");
        System.out.println("  Intermediate Track — Case Study: Trade Reconciliation");
        System.out.println();
    }
}
