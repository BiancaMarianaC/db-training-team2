package com.dbtraining.tradeflow.config;

import com.dbtraining.tradeflow.dto.TradeEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * KafkaConfig — TICKET-I118 (Day 9)
 * ============================================================================
 * WHAT:    Custom Kafka beans — DLT recoverer, error handler, consumer factory
 *          overrides.
 * HOW:     @Configuration class. Spring Boot auto-config covers most of it;
 *          this is where you override.
 * WHY:     Robust event pipelines need: retries, DLT for poison messages,
 *          observable error handling.
 * OBSERVE: A malformed message no longer crashes the consumer — it lands in
 *          `trade-events.DLT`.
 * ============================================================================
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    private final String dltTopic;

    public KafkaConfig(@Value("${tradeflow.kafka.topics.dlt:trade-events.DLT}") String dltTopic) {
        this.dltTopic = dltTopic;
    }

    @Bean
    public ConsumerFactory<String, TradeEvent> consumerFactory(KafkaProperties kafkaProperties,
                                                                 MeterRegistry meterRegistry) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.dbtraining.tradeflow.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TradeEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        DefaultKafkaConsumerFactory<String, TradeEvent> factory = new DefaultKafkaConsumerFactory<>(props);
        // Spring Boot only auto-attaches the Micrometer listener to ITS OWN
        // auto-configured ConsumerFactory bean (@ConditionalOnMissingBean) —
        // defining our own factory here (needed for the DLT/error-handling
        // setup below) skips that, so kafka_consumer_* metrics (the Grafana
        // "Consumer lag" panel from I121) never appeared without this.
        factory.addListener(new MicrometerConsumerListener<>(meterRegistry));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TradeEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, TradeEvent> consumerFactory,
            DefaultErrorHandler errorHandler) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, TradeEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setObservationEnabled(true);
        return factory;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3));

        // deserialization failures will never succeed on retry — skip straight to DLT
        handler.addNotRetryableExceptions(
                DeserializationException.class,
                IllegalArgumentException.class);
        return handler;
    }

    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name(dltTopic).partitions(1).replicas(1).build();
    }
}
