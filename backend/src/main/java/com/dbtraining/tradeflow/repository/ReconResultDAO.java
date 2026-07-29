package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.model.DiscrepancyType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ============================================================================
 * ReconResultDAO — TICKET-I046 (Day 4)
 * ============================================================================
 * WHAT:    JDBC DAO for recon_results.
 * HOW:     PreparedStatement + try-with-resources.
 * WHY:     The matching engine on Day 3 writes results here.
 * ============================================================================
 */
public class ReconResultDAO {

    private static final String INSERT_SQL = """
            INSERT INTO recon_breaks (trade_id, discrepancy_type, status)
            VALUES (?, ?, ?)
            """;

    private static final String FIND_BY_TRADE_ID_SQL = """
            SELECT id, trade_id, discrepancy_type, status, created_at, resolved_at
            FROM recon_breaks
            WHERE trade_id = ?
            ORDER BY created_at DESC, id DESC
            """;

    private static final String FIND_UNRESOLVED_SQL = """
            SELECT id, trade_id, discrepancy_type, status, created_at, resolved_at
            FROM recon_breaks
            WHERE status = ?
            ORDER BY created_at DESC, id DESC
            """;

    private final DataSource dataSource;

    public ReconResultDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource required");
    }

    public long insert(ReconResult result) {
        Objects.requireNonNull(result, "result required");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, result.getTradeId());
            statement.setString(2, result.getDiscrepancyType().name());
            statement.setString(3, result.getStatus());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
                throw new IllegalStateException("insert returned no generated key");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "ReconResultDAO.insert failed for trade " + result.getTradeId(), exception);
        }
    }

    public List<ReconResult> findByTradeId(long tradeId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_TRADE_ID_SQL)) {
            statement.setLong(1, tradeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapRows(resultSet);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("findByTradeId failed: " + tradeId, exception);
        }
    }

    public List<ReconResult> findUnresolved() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_UNRESOLVED_SQL)) {
            statement.setString(1, "OPEN");
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapRows(resultSet);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("findUnresolved failed", exception);
        }
    }

    private static List<ReconResult> mapRows(ResultSet resultSet) throws SQLException {
        List<ReconResult> results = new ArrayList<>();
        while (resultSet.next()) {
            results.add(mapRow(resultSet));
        }
        return results;
    }

    private static ReconResult mapRow(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        Timestamp resolvedAt = resultSet.getTimestamp("resolved_at");
        return ReconResult.builder()
                .tradeId(resultSet.getLong("trade_id"))
                .status(resultSet.getString("status"))
                .discrepancyType(DiscrepancyType.valueOf(
                        resultSet.getString("discrepancy_type")))
                .detectedAt(createdAt == null ? null : createdAt.toInstant())
                .resolvedAt(resolvedAt == null ? null : resolvedAt.toInstant())
                .build();
    }
}
