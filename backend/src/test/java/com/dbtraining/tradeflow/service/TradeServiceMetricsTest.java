package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.TradeRequest;
import com.dbtraining.tradeflow.repository.TradeRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceMetricsTest {

    @Mock private TradeRepository tradeRepository;

    private SimpleMeterRegistry meterRegistry;
    private TradeService tradeService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        tradeService = new TradeService(tradeRepository, meterRegistry);
    }

    @Test
    void createTrade_afterSuccessfulSave_incrementsCounter() {
        when(tradeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        tradeService.createTrade(request("TR-I079-SUCCESS"));

        assertThat(createdTrades()).isEqualTo(1.0);
    }

    @Test
    void createTrade_whenSaveFails_doesNotIncrementCounter() {
        when(tradeRepository.save(any())).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> tradeService.createTrade(request("TR-I079-FAIL")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
        assertThat(createdTrades()).isZero();
    }

    private double createdTrades() {
        return meterRegistry.get("tradeflow_trades_created_total").counter().count();
    }

    private static TradeRequest request(String tradeRef) {
        return new TradeRequest(
                tradeRef,
                1L,
                1L,
                new BigDecimal("100"),
                new BigDecimal("25.50"),
                LocalDate.of(2026, 7, 31));
    }
}
