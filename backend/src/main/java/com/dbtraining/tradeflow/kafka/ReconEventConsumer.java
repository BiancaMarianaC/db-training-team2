package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;
import com.dbtraining.tradeflow.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * ReconEventConsumer — TICKET-I117
 * ============================================================================
 * WHAT:    On every CREATED TradeEvent, triggers reconciliation for that
 *          trade.
 * HOW:     @KafkaListener with consumer group `recon-group`. Calls
 *          ReconciliationService.runForTrade(tradeRef).
 * WHY:     Real-time recon — the moment a trade lands, we know if it has a
 *          counterpart.
 * ============================================================================
 */
@Component
public class ReconEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReconEventConsumer.class);

    private final ReconciliationService reconService;

    public ReconEventConsumer(ReconciliationService reconService) {
        this.reconService = reconService;
    }

    @KafkaListener(
            topics = "${tradeflow.kafka.topics.trades}",
            groupId = "recon-group")
    public void onEvent(TradeEvent event) {
        if (event.action() != TradeEvent.Action.CREATED) {
            return;
        }
        log.info("Reconciling tradeRef={}", event.tradeRef());
        reconService.runForTrade(event.tradeRef());
    }
}
