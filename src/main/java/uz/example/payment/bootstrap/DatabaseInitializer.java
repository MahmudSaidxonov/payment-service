package uz.example.payment.bootstrap;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.regex.Pattern;

@Slf4j
public class DatabaseInitializer {

    private static final Pattern DB_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 3000L;

    public static void initialize() {
        String host = getEnvOrDefault("DB_HOST", "localhost");
        String port = getEnvOrDefault("DB_PORT", "5432");
        String dbName = getEnvOrDefault("DB_NAME", "payment");
        String user = getEnvOrDefault("DB_USER", "payment_user");
        String password = getEnvOrDefault("DB_PASSWORD", "change_me");

        if (!DB_NAME.matcher(dbName).matches()) {
            throw new IllegalArgumentException("Invalid database name: " + dbName + ". Must match ^[a-zA-Z0-9_]+$");
        }

        String adminUrl = String.format("jdbc:postgresql://%s:%s/postgres", host, port);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try (Connection connection = DriverManager.getConnection(adminUrl, user, password)) {
                log.info("Connected to PostgreSQL system database on attempt {}", attempt);
                ensureDatabaseExists(connection, dbName);
                return;
            } catch (Exception e) {
                log.warn("PostgreSQL is not ready yet (attempt {}/{}): {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    throw new IllegalStateException(
                            "Failed to connect to PostgreSQL after " + MAX_RETRIES + " attempts. Stopping application.", e);
                }
                sleep();
            }
        }

    }

    private static void ensureDatabaseExists(Connection connection, String dbName) throws Exception {
        String checkSql = "SELECT 1 FROM pg_database WHERE datname = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    log.info("Database '{}' already exists.", dbName);
                    return;
                }
            }
        }

        String createSql = "CREATE DATABASE \"" + dbName + "\"";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createSql);
            log.info("Database '{}' created successfully.", dbName);
        }
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Database initialization interrupted", ie);
        }
    }

}