package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * TradeRepository — TICKET-I060 (Day 5 — Spring Data JPA)
 * ============================================================================
 * WHAT:    JPA repository for the Trade entity — replaces Day 4's JDBC DAO.
 * HOW:     Extend JpaRepository<Trade, Long>; Spring generates the
 *          implementation at runtime.
 * WHY:     Less code, automatic pagination, derived queries from method names.
 * OBSERVE: No SQL written by hand — Spring Data infers it from the method
 *          name (`findByStatus`, `findByTradeDateBetween`).
 * ============================================================================
 */
public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByStatus(TradeStatus status);

    Page<Trade> findByStatus(TradeStatus status, Pageable pageable);

    List<Trade> findByTradeDateBetween(LocalDate from, LocalDate to);

    Optional<Trade> findByTradeRef(String tradeRef);
}
