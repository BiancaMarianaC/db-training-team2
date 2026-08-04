package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * TradeEventConsumer — TICKET-I116
 * ============================================================================
 * WHAT:    Simple log-only consumer to confirm the pipeline works.
 * HOW:     @KafkaListener on the `trade-events` topic.
 * WHY:     Smoke test before the more interesting Recon + Audit consumers
 *          (I117, I119) take action.
 * OBSERVE: Log line "Received TradeEvent[tradeRef=..., action=CREATED]"
 *          for every published event.
 * ============================================================================
 */
@Component
public class TradeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TradeEventConsumer.class);

    @KafkaListener(
            topics = "${tradeflow.kafka.topics.trades}",
            groupId = "trade-log-group")
    public void consume(TradeEvent event) {
        log.info("Received TradeEvent[tradeRef={}, action={}]",
                event.tradeRef(), event.action());
    }
}
