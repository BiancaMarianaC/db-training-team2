package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.dto.TradeRequest;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.repository.TradeRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * TradeService — TICKET-I041..I043 + TICKET-I062
 * ============================================================================
 * WHAT:    Business-logic facade for Trade operations.
 *          Day 4: HashMap-backed + Streams pipelines.
 *          Day 5: rewritten to use TradeRepository (Spring Data JPA).
 *          Day 6: also publishes TradeEvent to Kafka (TICKET-I115).
 * HOW:     @Service from Day 1 (so Spring can wire it into controllers).
 *          Day-1 default is a no-op stub — controllers bypass it via
 *          JdbcTemplate. Day-4 onward, students replace the stubs.
 * WHY:     Controllers stay thin — all rules and persistence live here.
 * OBSERVE: Switching from HashMap to JPA on Day 5 should NOT require changing
 *          callers (controller code stays the same).
 * ============================================================================
 *
 *  TICKET-I041: refactor in-memory store to Map<String, BaseTrade>.
 *  TICKET-I042: Streams pipeline — sumByCounterparty.
 *  TICKET-I043: Streams pipeline — topNByValue.
 *  TICKET-I062: rewrite using JPA repositories + DTOs (Day 5).
 * ============================================================================
 */
@Service
public class TradeService {

    // TICKET-I041: in-memory store as HashMap keyed by tradeRef.
    private final Map<String, BaseTrade> tradesByRef = new HashMap<>();

    // TICKET-I062: JPA repository, used only by createTrade() for now.
    // getAllTrades/addTrade/sumByCounterparty/topNByValue stay on the
    // HashMap<String, BaseTrade> above — BaseTrade and Trade are separate
    // type hierarchies, so migrating those would be a bigger change than
    // this ticket's TODO (createTrade only) asks for.
    private final TradeRepository tradeRepository;
    private final Counter tradesCreatedCounter;

    public TradeService(TradeRepository tradeRepository, MeterRegistry meterRegistry) {
        this.tradeRepository = tradeRepository;
        this.tradesCreatedCounter = Counter.builder("tradeflow_trades_created_total")
                .description("Total trades successfully created via POST /api/v1/trades")
                .register(meterRegistry);

        for (TradeStatus status : TradeStatus.values()) {
            Gauge.builder("tradeflow_trades_by_status",
                            tradeRepository,
                            r -> (double) r.countByStatus(status))
                    .description("Live count of trades per status")
                    .tag("status", status.name())
                    .register(meterRegistry);
        }
    }

    public Collection<BaseTrade> getAllTrades() {
        return Collections.unmodifiableCollection(tradesByRef.values());
    }

    public void addTrade(BaseTrade trade) {
        String tradeRef = trade.getTradeRef();
        if (tradesByRef.containsKey(tradeRef)) {
            throw new IllegalStateException("Duplicate tradeRef: " + tradeRef);
        }
        tradesByRef.put(tradeRef, trade);
    }

    /**
     * Sum notional (quantity * price) per counterparty across MATCHED trades only.
     */
    public Map<Long, BigDecimal> sumByCounterparty() {
        return tradesByRef.values().stream()
                .filter(t -> t.getStatus() == TradeStatus.MATCHED)
                .collect(Collectors.groupingBy(
                        BaseTrade::getCounterpartyId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                BaseTrade::getNotional,
                                BigDecimal::add)));
    }

    /**
     * Top N trades by notional value (quantity * price), descending.
     */
    public List<BaseTrade> topNByValue(int n) {
        return tradesByRef.values().stream()
                .sorted(Comparator.comparing(BaseTrade::getNotional).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    /**
     * TICKET-I068: list trades, optionally filtered by status and/or a
     * trade-date range. If only one of from/to is given (not both), the date
     * filter is skipped rather than guessing an open-ended range.
     */
    public List<TradeDto> getTrades(TradeStatus status, LocalDate from, LocalDate to) {
        List<Trade> trades;
        boolean hasDateRange = from != null && to != null;

        if (status != null && hasDateRange) {
            trades = tradeRepository.findByTradeDateBetween(from, to).stream()
                    .filter(t -> t.getStatus() == status)
                    .collect(Collectors.toList());
        } else if (status != null) {
            trades = tradeRepository.findByStatus(status);
        } else if (hasDateRange) {
            trades = tradeRepository.findByTradeDateBetween(from, to);
        } else {
            trades = tradeRepository.findAll();
        }

        return trades.stream().map(TradeService::toDto).collect(Collectors.toList());
    }

    /**
     * TICKET-I062: converts the inbound request into a Trade entity, saves it
     * via TradeRepository, and maps the persisted entity to a TradeDto.
     * TradeEvent publishing to Kafka is TICKET-I115 (Day 6) — not here yet.
     */
    public TradeDto createTrade(TradeRequest request) {
        Trade trade = Trade.builder()
                .tradeRef(request.tradeRef())
                .instrumentId(request.instrumentId())
                .counterpartyId(request.counterpartyId())
                .quantity(request.quantity())
                .price(request.price())
                .tradeDate(request.tradeDate())
                .build();

        Trade saved = tradeRepository.save(trade);
        tradesCreatedCounter.increment();
        return toDto(saved);
    }

    /**
     * TICKET-I070: loads the trade, applies the status transition via
     * Trade.updateStatus() (the one field allowed to change after creation),
     * and saves it back through TradeRepository.
     */
    public TradeDto updateStatus(Long id, TradeStatus newStatus) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Trade not found: " + id));
        trade.updateStatus(newStatus);
        Trade saved = tradeRepository.save(trade);
        return toDto(saved);
    }

    private static TradeDto toDto(Trade trade) {
        return new TradeDto(
                trade.getId(),
                trade.getTradeRef(),
                trade.getInstrumentId(),
                trade.getCounterpartyId(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getTradeDate(),
                trade.getStatus(),
                trade.getCreatedAt());
    }

    /**
     * TICKET-I071: soft delete just sets the trade's status to CANCELLED,
     * reusing the same transition path as updateStatus() (TICKET-I070) — the
     * row stays in the database, nothing is actually deleted.
     *
     * Not done here: writing an AuditLog row for this change. The AuditLog
     * entity exists (TICKET-I059), but there's no AuditLogRepository or
     * service to write through yet, so that part is left for whichever
     * ticket adds that infrastructure.
     */
    public void softDelete(Long id) {
        updateStatus(id, TradeStatus.CANCELLED);
    }
}
