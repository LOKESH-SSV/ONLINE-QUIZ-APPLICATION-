package com.collegequiz.redesign;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Central SQLite connection factory for the redesigned application.
 * The V2 database is intentionally separate from the legacy database.
 */
public final class DatabaseManager {
    private static final String URL = "jdbc:sqlite:online_quiz_v2.db";

    private DatabaseManager() { }

    public static Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 10000");
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
        return connection;
    }

    public static void configureDatabase() throws SQLException {
        try (Connection connection = open();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
        }
    }
}