package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.config.KafkaConfig;
import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.dto.TradeEvent;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.service.AuditService;
import com.dbtraining.tradeflow.service.ReconciliationService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * ============================================================================
 * TradeEventConsumerIntegrationTest — TICKET-I122 (Day 9)
 * ============================================================================
 * WHAT:    Round-trip check with an embedded Kafka broker: publish a
 *          TradeEvent, confirm the recon + audit consumer groups both
 *          received it (and that UPDATED events skip recon but still audit).
 * ============================================================================
 */
@SpringBootTest(classes = {
        KafkaConfig.class,
        TradeEventProducer.class,
        TradeEventConsumer.class,
        ReconEventConsumer.class,
        AuditEventConsumer.class
})
@EmbeddedKafka(partitions = 1, topics = { "trade-events", "trade-events.DLT" })
@Import(TradeEventConsumerIntegrationTest.TestKafkaConfig.class)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "tradeflow.kafka.topics.trades=trade-events",
        "tradeflow.kafka.topics.dlt=trade-events.DLT",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
@DirtiesContext
class TradeEventConsumerIntegrationTest {

    @Autowired
    private TradeEventProducer producer;

    @MockBean
    private ReconciliationService reconciliationService;

    @MockBean
    private AuditService auditService;

    @Test
    void publishedEvent_isReceivedByBothConsumerGroups() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(reconciliationService).runForTrade(eq("TRD-IT-0001"));
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(auditService).record(any(TradeEvent.class));

        TradeEvent event = new TradeEvent(
                "TRD-IT-0001",
                TradeEvent.Action.CREATED,
                Instant.now(),
                samplePayload("TRD-IT-0001"));

        producer.publish(event);

        assertTrue(latch.await(10, TimeUnit.SECONDS),
                "expected both recon and audit consumers to run within 10s");
        verify(reconciliationService).runForTrade(eq("TRD-IT-0001"));
        verify(auditService).record(any(TradeEvent.class));
    }

    @Test
    void updatedEvent_skipsReconButStillAudits() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(auditService).record(any(TradeEvent.class));

        TradeEvent event = new TradeEvent(
                "TRD-IT-0002",
                TradeEvent.Action.UPDATED,
                Instant.now(),
                samplePayload("TRD-IT-0002"));

        producer.publish(event);

        assertTrue(latch.await(10, TimeUnit.SECONDS),
                "expected audit consumer to run within 10s");
        verify(auditService).record(any(TradeEvent.class));
        verifyNoInteractions(reconciliationService);
    }

    private static TradeDto samplePayload(String tradeRef) {
        return new TradeDto(100L, tradeRef, 1L, 1L,
                new BigDecimal("100"), new BigDecimal("245.50"),
                LocalDate.of(2026, 3, 1), TradeStatus.PENDING, Instant.now());
    }

    @TestConfiguration
    @EnableConfigurationProperties(KafkaProperties.class)
    static class TestKafkaConfig {
        // KafkaConfig.consumerFactory() needs a MeterRegistry (for the
        // consumer-side Micrometer listener) — the slimmed @SpringBootTest
        // context here doesn't auto-configure one like the full app does.
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        KafkaTemplate<String, TradeEvent> kafkaTemplate(ProducerFactory<String, TradeEvent> pf) {
            return new KafkaTemplate<>(pf);
        }

        @Bean
        ProducerFactory<String, TradeEvent> producerFactory(KafkaProperties props) {
            return new DefaultKafkaProducerFactory<>(props.buildProducerProperties());
        }

        @Bean
        KafkaTemplate<String, Object> dltKafkaTemplate(KafkaProperties props) {
            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props.buildProducerProperties()));
        }
    }
}
