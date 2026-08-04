package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.Discrepancy;
import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.dto.ReconResultDto;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.AuditLog;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.repository.AuditLogRepository;
import com.dbtraining.tradeflow.repository.ReconResultRepository;
import com.dbtraining.tradeflow.exception.TradeNotFoundException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * ReconciliationService — TICKET-I034 + TICKET-I035 + TICKET-I036
 * ============================================================================
 * WHAT:    The heart of the system. Compares internal vs external trade lists,
 *          classifies discrepancies, persists results.
 * HOW:     Pure-Java on Day 3 (matchTrades + generateReport). On Day 5 this
 *          becomes a @Service injected with TradeRepository + ReconResultRepository.
 * WHY:     Single class with one job — find breaks. Easy to unit-test
 *          (Day 4 tests target this directly).
 * OBSERVE: Given the same input twice, the output is identical (pure function
 *          property — important for testability).
 *
 *  TICKET-I034: matchTrades(internal, external) -> ReconReport
 *  TICKET-I035: classify each pair: PRICE_MISMATCH / QUANTITY_MISMATCH /
 *               DATE_MISMATCH / MISSING_TRADE
 *  TICKET-I036: generateReport() -> ReconSummary
 * ============================================================================
 *
 * HINTS:
 *  - Build a Map<String, BaseTrade> externalByRef before the loop — O(1) lookup
 *    beats O(n²) nested iteration.
 *  - BigDecimal comparisons: NEVER `.equals()` (1.0 != 1.00). Use `compareTo() == 0`.
 *  - One trade can have multiple discrepancy types — your DTO must allow a List.
 *  - Keep this class < 200 lines. Pull helpers into private methods.
 * ============================================================================
 */
@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final ReconResultRepository reconResultRepository;
    private final Timer reconRunTimer;
    private final AuditLogRepository auditLogRepository;
    private final Counter reconResolvedCounter;

    public ReconciliationService(
        ReconResultRepository reconResultRepository,
        AuditLogRepository auditLogRepository,
        MeterRegistry meterRegistry
    ) {

        this.reconResultRepository = reconResultRepository;
        this.auditLogRepository = auditLogRepository;
        this.reconRunTimer = Timer.builder("tradeflow_recon_run_seconds")
                .description("Time taken for a full reconciliation run")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);
        this.reconResolvedCounter = Counter.builder("tradeflow_recon_resolved_total")
                .description("Count of reconciliation breaks marked RESOLVED")
                .register(meterRegistry);
    }

    /**
     * TICKET-I079: not implemented yet. tradeDAO.findAll() returns
     * List<Trade>, but matchTrades() (below) works on List<BaseTrade> —
     * Trade and BaseTrade are separate type hierarchies (same issue as
     * TICKET-I062), so this can't just forward to matchTrades() as-is.
     * There's also no real "external feed" source wired up yet, so it's
     * not clear what runForAll() should compare the internal trades
     * against. Left as an explicit failure instead of guessing, so it
     * doesn't silently do the wrong thing (e.g. flagging every trade as
     * a discrepancy).
     * 
     * UPDATE (TICKET-I078):
     * TradeDAO dependency removed because it was unused.
     * Persistence is now handled through JPA repositories.
     */
    @Transactional(readOnly = true)
    public ReconSummary runForAll() {
        return reconRunTimer.record(() -> {
            int matched = Math.toIntExact(
                    reconResultRepository.countByStatus(ReconResult.Status.RESOLVED));
            int unmatched = Math.toIntExact(
                    reconResultRepository.countByStatus(ReconResult.Status.OPEN));

            Map<DiscrepancyType, Integer> breakdown = new EnumMap<>(DiscrepancyType.class);
            for (DiscrepancyType type : DiscrepancyType.values()) {
                breakdown.put(type, 0);
            }
            for (ReconResult result
                    : reconResultRepository.findByStatus(ReconResult.Status.OPEN)) {
                breakdown.merge(result.getDiscrepancyType(), 1, Integer::sum);
            }

            int total = matched + unmatched;
            return new ReconSummary(total, total, matched, unmatched,
                    Collections.unmodifiableMap(breakdown));
        });
    }

    /**
     * TICKET-I117: triggered by ReconEventConsumer on every CREATED
     * TradeEvent. Real per-trade reconciliation needs an external feed to
     * compare against, which doesn't exist yet (same gap as runForAll,
     * see TICKET-I079 above) — so this only logs that recon fired for the
     * trade for now. Wire up a real comparison once an external source is
     * available; don't fabricate one here just to produce a ReconResult.
     */
    public void runForTrade(String tradeRef) {
        log.info("Recon triggered for tradeRef={} (no external feed wired yet — no-op)",
                tradeRef);
    }

    @Transactional(readOnly = true)
    public Page<ReconResultDto> listBreaks(ReconResult.Status status,
                                           Long counterpartyId,
                                           Pageable pageable) {
        Page<ReconResult> results = counterpartyId == null
                ? reconResultRepository.findByStatus(status, pageable)
                : reconResultRepository.findByStatusAndCounterpartyId(
                        status, counterpartyId, pageable);
        return results.map(ReconResultDto::from);
    }

    @Transactional
    public void resolveBreak(Long id, String changedBy) {
        ReconResult result = reconResultRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException(
                        "Recon break " + id + " not found"));
        if (result.getStatus() == ReconResult.Status.RESOLVED) {
            return;
        }

        String previousStatus = result.getStatus().name();
        result.resolve();
        auditLogRepository.save(AuditLog.builder()
                .tableName("recon_breaks")
                .operation(AuditLog.Operation.U)
                .rowPk(id)
                .beforeData("{\"status\":\"" + previousStatus + "\",\"resolvedAt\":null}")
                .afterData("{\"status\":\"RESOLVED\",\"resolvedAt\":\""
                        + result.getResolvedAt() + "\"}")
                .changedBy(changedBy)
                .build());
        reconResolvedCounter.increment();
    }

    /**
     * TODO(TICKET-I034 + TICKET-I035):
     *   Compare two lists of trades by tradeRef. Return a ReconReport with:
     *     - matched: trades present + identical on both sides
     *     - discrepancies: list of (tradeRef, List<DiscrepancyType>) entries
     */
    public ReconReport matchTrades(List<BaseTrade> internal, List<BaseTrade> external) {
        Objects.requireNonNull(internal, "internal list required");
        Objects.requireNonNull(external, "external list required");

        Map<String, BaseTrade> externalByRef = external.stream()
                .collect(Collectors.toMap(
                    BaseTrade::getTradeRef, trade -> trade,
                    (first, duplicate) -> {
                        throw new IllegalArgumentException(
                                "Duplicate external tradeRef: " + first.getTradeRef()
                        );
                    }
                ));

        List<BaseTrade> matched = new ArrayList<>();
        List<Discrepancy> discrepancies = new ArrayList<>();

        for (BaseTrade internalTrade : internal) {
            BaseTrade externalTrade = externalByRef.remove(internalTrade.getTradeRef());
            if (externalTrade == null) {
                discrepancies.add(new Discrepancy(
                        internalTrade.getTradeRef(),
                        List.of(DiscrepancyType.MISSING_TRADE)));
                continue;
            }

            List<DiscrepancyType> differences = classify(internalTrade, externalTrade);
            if (differences.isEmpty()) {
                matched.add(internalTrade);
            } else {
                discrepancies.add(new Discrepancy(internalTrade.getTradeRef(), differences));
            }
        }

        for (BaseTrade externalOnlyTrade : externalByRef.values()) {
            discrepancies.add(new Discrepancy(
                    externalOnlyTrade.getTradeRef(),
                    List.of(DiscrepancyType.MISSING_TRADE)));
        }

        return new ReconReport(internal.size(), external.size(), matched, discrepancies);
    }

    private List<DiscrepancyType> classify(BaseTrade internalTrade, BaseTrade externalTrade) {
        List<DiscrepancyType> diffs = new ArrayList<>(2);
        if (internalTrade.getPrice().compareTo(externalTrade.getPrice()) != 0)
            diffs.add(DiscrepancyType.PRICE_MISMATCH);
        if (internalTrade.getQuantity().compareTo(externalTrade.getQuantity()) != 0)
            diffs.add(DiscrepancyType.QUANTITY_MISMATCH);
        if (!Objects.equals(internalTrade.getTradeDate(), externalTrade.getTradeDate()))
            diffs.add(DiscrepancyType.DATE_MISMATCH);
        return diffs;
    }

    public ReconSummary generateReport(ReconReport report) {
        Objects.requireNonNull(report, "report required");

        Map<DiscrepancyType, Integer> breakdown = new EnumMap<>(DiscrepancyType.class);
        for (DiscrepancyType t : DiscrepancyType.values()) breakdown.put(t, 0);
        for (Discrepancy d : report.discrepancies()) {
            for (DiscrepancyType t : d.types()) breakdown.merge(t, 1, Integer::sum);
        }
        return new ReconSummary(
                report.totalInternal(),
                report.totalExternal(),
                report.matched().size(),
                report.discrepancies().size(),
                Collections.unmodifiableMap(breakdown));
    }

    public String render(ReconSummary s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Reconciliation summary\n----------------------\n")
        .append(String.format("  Internal trades : %d%n", s.totalInternal()))
        .append(String.format("  External trades : %d%n", s.totalExternal()))
        .append(String.format("  Matched         : %d%n", s.matchedCount()))
        .append(String.format("  With breaks     : %d%n", s.unmatchedCount()))
        .append("  Breakdown:\n");
        s.breakdownByType().forEach((type, count) ->
                sb.append(String.format("    - %-20s %d%n", type, count)));
        return sb.toString();
    }
}
