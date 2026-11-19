package com.collegequiz.redesign.service;

import com.collegequiz.redesign.dao.UserDAO;
import com.collegequiz.redesign.model.User;
import com.collegequiz.redesign.security.PasswordHasher;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

public final class AuthService {
    private final UserDAO users = new UserDAO();

    public User login(String email, String password) throws SQLException {
        if (email == null || password == null || email.isBlank() || password.isEmpty()) {
            throw new IllegalArgumentException("Enter your email and password.");
        }
        Optional<UserDAO.UserRecord> found = users.findForLogin(email.toLowerCase(Locale.ROOT));
        if (found.isEmpty() || !found.get().active()
                || !PasswordHasher.verify(password, found.get().passwordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return found.get().user();
    }

    public User register(String fullName, String email, String password,
                         String confirmation, String role) throws SQLException {
        String name = fullName == null ? "" : fullName.trim();
        String mail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (name.length() < 2) throw new IllegalArgumentException("Enter your full name.");
        if (!mail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        }
        if (!password.equals(confirmation)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (!"STUDENT".equals(role) && !"TEACHER".equals(role)) {
            throw new IllegalArgumentException("Choose Student or Teacher.");
        }
        try {
            return users.create(name, mail, PasswordHasher.hash(password), role);
        } catch (SQLException exception) {
            if (exception.getMessage() != null
                    && exception.getMessage().toLowerCase(Locale.ROOT).contains("unique")) {
                throw new IllegalArgumentException("An account with this email already exists.");
            }
            throw exception;
        }
    }
}