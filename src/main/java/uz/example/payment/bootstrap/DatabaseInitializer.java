package uz.example.payment.bootstrap;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.regex.Pattern;

@Slf4j
public class DatabaseInitializer {

    private static final Pattern DB_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 3000L;

    public static void initialize() {
        loadDotEnvIntoSystemProperties();

        String port = getEnvOrDefault("DB_PORT", "5432");
        String dbName = getEnvOrDefault("DB_NAME", "payment");
        String host = getRequiredEnv("DB_HOST");
        String user = getRequiredEnv("DB_USER");
        String password = getRequiredEnv("DB_PASSWORD");

        if (!DB_NAME.matcher(dbName).matches()) {
            throw new IllegalArgumentException("Invalid database name: " + dbName + ". Must match ^[a-zA-Z0-9_]+$");
        }

        String adminUrl = String.format("jdbc:postgresql://%s:%s/postgres", host, port);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try (Connection connection = DriverManager.getConnection(adminUrl, user, password)) {
                log.info("Connected to PostgreSQL system database on attempt {}", attempt);
                ensureDatabaseExists(connection, dbName);
                return;
            } catch (SQLException e) {
                log.warn("PostgreSQL is not ready yet (attempt {}/{}): {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    throw new IllegalStateException(
                            "Failed to connect to PostgreSQL after " + MAX_RETRIES + " attempts. Stopping application.", e);
                }
                sleep();
            }
        }
}

    private static void ensureDatabaseExists(Connection connection, String dbName) throws SQLException {
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
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createSql);
            log.info("Database '{}' created successfully.", dbName);
        }
    }

    private static String getRequiredEnv(String key) {
        String value = resolveValue(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable is not specified");
        }
        return value;
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = resolveValue(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static String resolveValue(String key) {
        String sysEnv = System.getenv(key);
        if (sysEnv != null && !sysEnv.isBlank()) {
            return sysEnv;
        }
        return System.getProperty(key);
    }

    private static void loadDotEnvIntoSystemProperties() {
        Path path = Path.of(".env");
        if (!Files.exists(path)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                if (idx > 0) {
                    String key = trimmed.substring(0, idx).trim();
                    String value = trimmed.substring(idx + 1).trim();
                    if (System.getenv(key) == null && System.getProperty(key) == null) {
                        System.setProperty(key, value);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Could not read .env file: {}", e.getMessage());
        }
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