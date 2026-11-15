package com.collegequiz;

import java.sql.*;

public final class DB {
    private static final String URL = "jdbc:sqlite:online_quiz.db";
    private DB() {}

    public static Connection getConnection() throws SQLException {
        Connection con = DriverManager.getConnection(URL);
        try (Statement st = con.createStatement()) { st.execute("PRAGMA foreign_keys=ON"); }
        return con;
    }

    public static void initialize() throws SQLException {
        try (Connection con=getConnection(); Statement st=con.createStatement()) {
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
                  difficulty TEXT DEFAULT 'MEDIUM',
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
            String[] cats={"Java","C++","DBMS","Computer Networks"};
            try(PreparedStatement ps=con.prepareStatement("INSERT OR IGNORE INTO categories(category_name) VALUES(?)")){
                for(String cat:cats){ps.setString(1,cat);ps.addBatch();} ps.executeBatch();
            }
            // Remove only the old built-in demo MCQs. Teachers now create every quiz question.
            String[] demoQuestions={
                "Which keyword is used to inherit a class in Java?",
                "Which method is the entry point of a Java application?",
                "Which collection does not allow duplicate elements?",
                "Which JDBC object is suited to parameterized SQL queries?",
                "What does JVM stand for?",
                "Which symbol ends a C++ statement?",
                "Which SQL command retrieves rows from a table?",
                "Which protocol is used to browse secure websites?"
            };
            try(PreparedStatement ps=con.prepareStatement("DELETE FROM questions WHERE question_text=? AND question_id NOT IN (SELECT question_id FROM quiz_questions)")){
                for(String q:demoQuestions){ps.setString(1,q);ps.addBatch();}ps.executeBatch();
            }
        }
    }
}