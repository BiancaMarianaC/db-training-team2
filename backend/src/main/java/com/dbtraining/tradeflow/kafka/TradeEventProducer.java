package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * TradeEventProducer — TICKET-I115 (Day 9)
 * ============================================================================
 * WHAT:    Publishes TradeEvent messages to Kafka topic `trade-events`.
 * HOW:     KafkaTemplate<String, TradeEvent>. Called from TradeService.
 * WHY:     Decouples "trade saved" from "recon ran" / "audit recorded" —
 *          new consumers can subscribe without touching the producer.
 * OBSERVE: Kafdrop (localhost:9000) shows the message immediately after
 *          POST /api/v1/trades succeeds.
 * ============================================================================
 *
 *  GOTCHA: a Kafka publish failure must never roll back the DB transaction.
 *          publish() only logs on failure via whenComplete, it never throws.
 * ============================================================================
 */
@Service
public class TradeEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TradeEventProducer.class);

    private final KafkaTemplate<String, TradeEvent> kafkaTemplate;
    private final String topic;

    public TradeEventProducer(KafkaTemplate<String, TradeEvent> kafkaTemplate,
                               @Value("${tradeflow.kafka.topics.trades}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(TradeEvent event) {
        // kafkaTemplate.send() can throw synchronously (e.g. a metadata-fetch
        // TimeoutException when no broker is reachable) as well as failing
        // the returned future asynchronously — both paths must only log,
        // never propagate, so a Kafka outage never turns a successful DB
        // write into a 500 for the caller.
        try {
            kafkaTemplate.send(topic, event.tradeRef(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish TradeEvent tradeRef={} action={}",
                                    event.tradeRef(), event.action(), ex);
                        } else if (log.isDebugEnabled()) {
                            log.debug("Published TradeEvent tradeRef={} -> partition={} offset={}",
                                    event.tradeRef(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception ex) {
            log.error("Failed to publish TradeEvent tradeRef={} action={}",
                    event.tradeRef(), event.action(), ex);
        }
    }
}
