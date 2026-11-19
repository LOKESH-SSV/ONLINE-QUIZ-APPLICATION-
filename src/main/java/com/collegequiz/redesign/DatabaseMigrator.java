package com.collegequiz.redesign;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/** Applies each SQL migration exactly once, tracked by schema version. */
public final class DatabaseMigrator {
    private static final int CURRENT_VERSION = 1;
    private DatabaseMigrator() { }

    public static void migrate() throws SQLException, IOException {
        DatabaseManager.configureDatabase();
        try (Connection connection = DatabaseManager.open()) {
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("""
                        CREATE TABLE IF NOT EXISTS schema_history (
                            version INTEGER PRIMARY KEY,
                            description TEXT NOT NULL,
                            applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                }
                int version = currentVersion(connection);
                if (version < 1) {
                    String sql = readResource("/db/migration/V1__fresh_schema.sql");
                    try (Statement statement = connection.createStatement()) {
                        for (String command : sql.split(";")) {
                            if (!command.isBlank()) statement.execute(command.trim());
                        }
                    }
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO schema_history(version, description) VALUES(?, ?)")) {
                        insert.setInt(1, 1);
                        insert.setString(2, "Fresh normalized quiz application schema");
                        insert.executeUpdate();
                    }
                }
                if (version > CURRENT_VERSION) {
                    throw new SQLException("Database schema is newer than this application.");
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException sqlException) throw sqlException;
                if (exception instanceof IOException ioException) throw ioException;
                throw new SQLException("Database migration failed.", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private static int currentVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_history")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private static String readResource(String path) throws IOException {
        try (InputStream input = DatabaseMigrator.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing database migration: " + path);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}