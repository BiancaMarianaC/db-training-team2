package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;
import com.dbtraining.tradeflow.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * AuditEventConsumer — TICKET-I119
 * ============================================================================
 * WHAT:    Writes one audit_log row per TradeEvent.
 * HOW:     @KafkaListener with consumer group `audit-group` — different
 *          from recon-group so it consumes in parallel.
 * WHY:     Audit is a parallel concern. If recon dies, audit still runs;
 *          if audit dies, recon still runs. Separate consumer groups give
 *          us that isolation for free.
 * OBSERVE: After each posted trade, `SELECT * FROM audit_log ORDER BY id DESC LIMIT 1;`
 *          returns the new row.
 * ============================================================================
 */
@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final AuditService auditService;

    public AuditEventConsumer(AuditService auditService) {
        this.auditService = auditService;
    }

    @KafkaListener(
            topics = "${tradeflow.kafka.topics.trades}",
            groupId = "audit-group")
    public void onEvent(TradeEvent event) {
        log.debug("Auditing tradeRef={} action={}", event.tradeRef(), event.action());
        auditService.record(event);
    }
}
