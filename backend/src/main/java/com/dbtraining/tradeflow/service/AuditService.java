package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.TradeEvent;
import com.dbtraining.tradeflow.model.AuditLog;
import com.dbtraining.tradeflow.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * AuditService — TICKET-I119 (Day 9)
 * ============================================================================
 * WHAT:    Persists one audit_log row per Kafka TradeEvent.
 * HOW:     Called from AuditEventConsumer (groupId=audit-group), a separate
 *          consumer group from recon-group so both run in parallel.
 * WHY:     An audit-DB hiccup never blocks the trade write itself — this
 *          only runs off the Kafka partition, async from the HTTP request.
 * ============================================================================
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(TradeEvent event) {
        if (event.action() == null || event.payload() == null) {
            log.warn("Skipping audit record for malformed TradeEvent (tradeRef={}, action={}, payload={})",
                    event.tradeRef(), event.action(), event.payload());
            return;
        }
        auditLogRepository.save(AuditLog.builder()
                .tableName("trades")
                .operation(toOperation(event.action()))
                .rowPk(event.payload().id())
                .afterData(toJson(event.payload()))
                .changedBy("kafka:" + event.action())
                .changedAt(event.timestamp())
                .build());
    }

    private AuditLog.Operation toOperation(TradeEvent.Action action) {
        return switch (action) {
            case CREATED -> AuditLog.Operation.I;
            case UPDATED -> AuditLog.Operation.U;
            case CANCELLED -> AuditLog.Operation.D;
        };
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize audit payload, storing null", ex);
            return null;
        }
    }
}
