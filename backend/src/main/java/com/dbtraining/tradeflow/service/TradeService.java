package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.dto.TradeRequest;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.repository.TradeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public TradeService(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
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

        return new TradeDto(
                saved.getId(),
                saved.getTradeRef(),
                saved.getInstrumentId(),
                saved.getCounterpartyId(),
                saved.getQuantity(),
                saved.getPrice(),
                saved.getTradeDate(),
                saved.getStatus(),
                saved.getCreatedAt());
    }

    public TradeDto updateStatus(Long id, TradeStatus newStatus) {
        // TODO(TICKET-I070): implement on Day 6.
        throw new UnsupportedOperationException("TICKET-I070");
    }

    public void softDelete(Long id) {
        // TODO(TICKET-I071): implement soft delete + audit log on Day 6.
        throw new UnsupportedOperationException("TICKET-I071");
    }
}
