package com.collegequiz.redesign.dao;

import com.collegequiz.redesign.DatabaseManager;
import com.collegequiz.redesign.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public final class UserDAO {
    public Optional<UserRecord> findForLogin(String email) throws SQLException {
        String sql = "SELECT user_id, full_name, email, password_hash, role, is_active "
                + "FROM users WHERE email = ?";
        try (Connection connection = DatabaseManager.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(new UserRecord(
                        new User(result.getInt("user_id"), result.getString("full_name"),
                                result.getString("email"), result.getString("role")),
                        result.getString("password_hash"), result.getInt("is_active") == 1));
            }
        }
    }

    public User create(String fullName, String email, String passwordHash, String role)
            throws SQLException {
        String sql = "INSERT INTO users(full_name, email, password_hash, role) VALUES(?,?,?,?)";
        try (Connection connection = DatabaseManager.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, fullName.trim());
            statement.setString(2, email.trim());
            statement.setString(3, passwordHash);
            statement.setString(4, role);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Could not create the account.");
                return new User(keys.getInt(1), fullName.trim(), email.trim(), role);
            }
        }
    }

    public record UserRecord(User user, String passwordHash, boolean active) { }
}