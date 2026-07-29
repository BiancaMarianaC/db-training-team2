package com.dbtraining.tradeflow.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * ============================================================================
 * DatabaseConfig — TICKET-I044 (Day 4 — pre-Spring Boot)
 * ============================================================================
 * WHAT:    Standalone connection pool config for Day-4 JDBC work.
 * HOW:     Build a HikariDataSource from environment / properties.
 * WHY:     Day 4 uses raw JDBC — without a pool you'd open + close a
 *          connection per call, which is brutal under any load.
 * OBSERVE: On Day 5 Spring Boot auto-configures the DataSource — you can
 *          delete this class OR keep it for the standalone TradeProcessor
 *          smoke run. Document your team's choice in the PR.
 * ============================================================================
 *
 *  TODO(TICKET-I044):
 *    public static DataSource dataSource() {
 *        HikariConfig cfg = new HikariConfig();
 *        cfg.setJdbcUrl(System.getenv().getOrDefault("JDBC_URL",
 *            "jdbc:postgresql://localhost:5432/tradeflow"));
 *        cfg.setUsername(System.getenv().getOrDefault("POSTGRES_USER", "tradeflow_user"));
 *        cfg.setPassword(System.getenv().getOrDefault("POSTGRES_PASSWORD", "changeme"));
 *        cfg.setMaximumPoolSize(10);
 *        cfg.setConnectionTimeout(5_000);
 *        return new HikariDataSource(cfg);
 *    }
 * ============================================================================
 */
public final class DatabaseConfig {

    private static volatile DataSource instance;

    private DatabaseConfig() {
    }

    public static DataSource dataSource() {
        DataSource local = instance;
        if (local == null) {
            synchronized (DatabaseConfig.class) {
                local = instance;
                if (local == null) {
                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl(System.getenv().getOrDefault(
                            "JDBC_URL", "jdbc:postgresql://localhost:5432/tradeflow"));
                    config.setUsername(System.getenv().getOrDefault(
                            "POSTGRES_USER", "tradeflow_user"));
                    config.setPassword(System.getenv().getOrDefault(
                            "POSTGRES_PASSWORD", "changeme"));
                    config.setMaximumPoolSize(10);
                    config.setConnectionTimeout(5_000);
                    config.setPoolName("tradeflow-jdbc");
                    instance = local = new HikariDataSource(config);
                }
            }
        }
        return local;
    }
}
