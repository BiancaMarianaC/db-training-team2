package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * ============================================================================
 * TradeDAO — TICKET-I045 (Day 4 — raw JDBC)
 * ============================================================================
 * WHAT:    Raw JDBC data-access object for trades.
 * HOW:     PreparedStatement everywhere — NEVER string concatenation.
 * WHY:     Day 4 builds JDBC by hand so you understand what JPA hides on Day 5.
 * OBSERVE: Day 5's TradeRepository (Spring Data) replaces this class entirely.
 *          On Day 5 you can either delete this file or keep it for comparison.
 * ============================================================================
 *
 * HINTS:
 *  - try-with-resources for Connection, PreparedStatement, ResultSet.
 *  - For insert + generated key: Statement.RETURN_GENERATED_KEYS.
 *  - Map ResultSet → Trade via Trade.builder()...build().
 *  - Wrap SQLException in a RuntimeException with context (which method failed).
 * ============================================================================
 */
public class TradeDAO {

    private static final String INSERT_SQL = """
            INSERT INTO trades
                (trade_ref, instrument_id, counterparty_id, quantity, price, trade_date, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String FIND_BY_REF_SQL = """
            SELECT id, trade_ref, instrument_id, counterparty_id, quantity, price,
                   trade_date, status, created_at
            FROM trades
            WHERE trade_ref = ?
            LIMIT 1
            """;

    private static final String FIND_ALL_SQL = """
            SELECT id, trade_ref, instrument_id, counterparty_id, quantity, price,
                   trade_date, status, created_at
            FROM trades
            ORDER BY trade_date DESC, id DESC
            """;

    private static final String UPDATE_STATUS_SQL = """
            UPDATE trades
            SET status = ?
            WHERE trade_ref = ?
            """;

    private final DataSource dataSource;

    public TradeDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource required");
    }

    public long insert(Trade trade) {
        Objects.requireNonNull(trade, "trade required");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, trade.getTradeRef());
            statement.setLong(2, trade.getInstrumentId());
            statement.setLong(3, trade.getCounterpartyId());
            statement.setBigDecimal(4, trade.getQuantity());
            statement.setBigDecimal(5, trade.getPrice());
            statement.setDate(6, Date.valueOf(trade.getTradeDate()));
            statement.setString(7, trade.getStatus().name());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
                throw new IllegalStateException("insert returned no generated key");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("insert failed: " + trade.getTradeRef(), exception);
        }
    }

    public Optional<Trade> findByRef(String tradeRef) {
        Objects.requireNonNull(tradeRef, "tradeRef required");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_REF_SQL)) {
            statement.setString(1, tradeRef);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(mapRow(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("findByRef failed: " + tradeRef, exception);
        }
    }

    public List<Trade> findAll() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            List<Trade> trades = new ArrayList<>();
            while (resultSet.next()) {
                trades.add(mapRow(resultSet));
            }
            return trades;
        } catch (SQLException exception) {
            throw new IllegalStateException("findAll failed", exception);
        }
    }

    public int updateStatus(String tradeRef, TradeStatus newStatus) {
        Objects.requireNonNull(tradeRef, "tradeRef required");
        Objects.requireNonNull(newStatus, "newStatus required");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS_SQL)) {
            statement.setString(1, newStatus.name());
            statement.setString(2, tradeRef);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("updateStatus failed: " + tradeRef, exception);
        }
    }

    private static Trade mapRow(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        return Trade.builder()
                .tradeRef(resultSet.getString("trade_ref"))
                .instrumentId(resultSet.getLong("instrument_id"))
                .counterpartyId(resultSet.getLong("counterparty_id"))
                .quantity(resultSet.getBigDecimal("quantity"))
                .price(resultSet.getBigDecimal("price"))
                .tradeDate(resultSet.getDate("trade_date").toLocalDate())
                .status(TradeStatus.valueOf(resultSet.getString("status")))
                .createdAt(createdAt == null ? null : createdAt.toInstant())
                .build();
    }
}
