package com.collegequiz;

import java.sql.*;

public final class DB {
    private static final String URL = "jdbc:sqlite:online_quiz.db";
    private DB() {}

    public static Connection getConnection() throws SQLException {
        Connection con = DriverManager.getConnection(URL);
        try (Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys=ON");
            st.execute("PRAGMA busy_timeout=5000");
        }
        return con;
    }

    public static void initialize() throws SQLException {
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users(
                  user_id INTEGER PRIMARY KEY AUTOINCREMENT, full_name TEXT NOT NULL,
                  email TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL,
                  role TEXT NOT NULL DEFAULT 'STUDENT' CHECK(role IN ('STUDENT','ADMIN')),
                  created_at TEXT DEFAULT CURRENT_TIMESTAMP)""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS categories(
                  category_id INTEGER PRIMARY KEY AUTOINCREMENT,
                  category_name TEXT NOT NULL UNIQUE)""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS questions(
                  question_id INTEGER PRIMARY KEY AUTOINCREMENT, category_id INTEGER NOT NULL,
                  question_text TEXT NOT NULL, option_a TEXT NOT NULL, option_b TEXT NOT NULL,
                  option_c TEXT NOT NULL, option_d TEXT NOT NULL,
                  correct_option TEXT NOT NULL CHECK(correct_option IN ('A','B','C','D')),
                  difficulty TEXT DEFAULT 'MEDIUM', topic TEXT DEFAULT '',
                  explanation TEXT DEFAULT '',
                  FOREIGN KEY(category_id) REFERENCES categories(category_id))""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS results(
                  result_id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL,
                  category_id INTEGER NOT NULL, total_questions INTEGER NOT NULL,
                  correct_answers INTEGER NOT NULL, score_percent REAL NOT NULL,
                  time_taken_seconds INTEGER NOT NULL, attempted_at TEXT DEFAULT CURRENT_TIMESTAMP,
                  FOREIGN KEY(user_id) REFERENCES users(user_id),
                  FOREIGN KEY(category_id) REFERENCES categories(category_id))""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS quizzes(
                  quiz_id INTEGER PRIMARY KEY AUTOINCREMENT, teacher_id INTEGER NOT NULL,
                  title TEXT NOT NULL, category_id INTEGER NOT NULL, duration_minutes INTEGER NOT NULL,
                  question_count INTEGER NOT NULL, access_code TEXT UNIQUE,
                  status TEXT NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','PUBLISHED','CLOSED')),
                  created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                  FOREIGN KEY(teacher_id) REFERENCES users(user_id),
                  FOREIGN KEY(category_id) REFERENCES categories(category_id))""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS quiz_questions(
                  quiz_id INTEGER NOT NULL, question_id INTEGER NOT NULL,
                  PRIMARY KEY(quiz_id,question_id),
                  FOREIGN KEY(quiz_id) REFERENCES quizzes(quiz_id) ON DELETE CASCADE,
                  FOREIGN KEY(question_id) REFERENCES questions(question_id))""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS attempts(
                  attempt_id INTEGER PRIMARY KEY AUTOINCREMENT, quiz_id INTEGER NOT NULL,
                  user_id INTEGER NOT NULL, total_questions INTEGER NOT NULL,
                  correct_answers INTEGER NOT NULL, score_percent REAL NOT NULL,
                  time_taken_seconds INTEGER NOT NULL, submitted_at TEXT DEFAULT CURRENT_TIMESTAMP,
                  FOREIGN KEY(quiz_id) REFERENCES quizzes(quiz_id),
                  FOREIGN KEY(user_id) REFERENCES users(user_id))""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS student_answers(
                  answer_id INTEGER PRIMARY KEY AUTOINCREMENT, attempt_id INTEGER NOT NULL,
                  question_id INTEGER NOT NULL, selected_option TEXT,
                  is_correct INTEGER NOT NULL DEFAULT 0,
                  FOREIGN KEY(attempt_id) REFERENCES attempts(attempt_id) ON DELETE CASCADE,
                  FOREIGN KEY(question_id) REFERENCES questions(question_id))""");

            // Safe, additive migrations for databases created by earlier project versions.
            ensureColumn(con, "questions", "difficulty", "TEXT DEFAULT 'MEDIUM'");
            ensureColumn(con, "questions", "topic", "TEXT DEFAULT ''");
            ensureColumn(con, "questions", "explanation", "TEXT DEFAULT ''");
            ensureColumn(con, "quizzes", "negative_marking_enabled", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(con, "quizzes", "negative_marks", "REAL NOT NULL DEFAULT 0");
            ensureColumn(con, "quizzes", "starts_at", "TEXT");
            ensureColumn(con, "quizzes", "ends_at", "TEXT");
            ensureColumn(con, "attempts", "started_at", "TEXT");
            ensureColumn(con, "attempts", "status", "TEXT NOT NULL DEFAULT 'SUBMITTED'");
            ensureColumn(con, "attempts", "incorrect_answers", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(con, "attempts", "unanswered_answers", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(con, "attempts", "raw_score", "REAL NOT NULL DEFAULT 0");
            ensureColumn(con, "attempts", "negative_marks_applied", "REAL NOT NULL DEFAULT 0");
            ensureColumn(con, "student_answers", "marked_for_review", "INTEGER NOT NULL DEFAULT 0");
            try (Statement update = con.createStatement()) {
                update.executeUpdate("UPDATE attempts SET started_at=COALESCE(started_at,submitted_at) WHERE started_at IS NULL");
            }
            try (Statement index = con.createStatement()) {
                index.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS ux_student_answers_attempt_question ON student_answers(attempt_id, question_id)");
            }

            String[] cats = {"Java", "C++", "DBMS", "Computer Networks"};
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT OR IGNORE INTO categories(category_name) VALUES(?)")) {
                for (String cat : cats) { ps.setString(1, cat); ps.addBatch(); }
                ps.executeBatch();
            }

            // Remove only known legacy demo MCQs that are not part of a published quiz.
            String[] demoQuestions = {
                "Which keyword is used to inherit a class in Java?",
                "Which method is the entry point of a Java application?",
                "Which collection does not allow duplicate elements?",
                "Which JDBC object is suited to parameterized SQL queries?",
                "What does JVM stand for?",
                "Which symbol ends a C++ statement?",
                "Which SQL command retrieves rows from a table?",
                "Which protocol is used to browse secure websites?"
            };
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM questions WHERE question_text=? AND question_id NOT IN (SELECT question_id FROM quiz_questions)")) {
                for (String q : demoQuestions) { ps.setString(1, q); ps.addBatch(); }
                ps.executeBatch();
            }
        }
    }

    private static void ensureColumn(Connection con, String table, String column, String definition)
            throws SQLException {
        boolean exists = false;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) if (column.equalsIgnoreCase(rs.getString("name"))) exists = true;
        }
        if (!exists) try (Statement st = con.createStatement()) {
            st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
