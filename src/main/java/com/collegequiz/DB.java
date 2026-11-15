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
            addSample(con,"Java","Which keyword is used to inherit a class in Java?","implements","extends","inherits","super","B");
            addSample(con,"Java","Which method is the entry point of a Java application?","start()","run()","main()","init()","C");
            addSample(con,"Java","Which collection does not allow duplicate elements?","ArrayList","LinkedList","HashSet","Vector","C");
            addSample(con,"Java","Which JDBC object is suited to parameterized SQL queries?","Statement","PreparedStatement","ResultSet","DriverManager","B");
            addSample(con,"Java","What does JVM stand for?","Java Variable Method","Java Virtual Machine","Joint Virtual Memory","Java Verified Module","B");
            addSample(con,"C++","Which symbol ends a C++ statement?",":",";",".",",","B");
            addSample(con,"DBMS","Which SQL command retrieves rows from a table?","GET","OPEN","SELECT","FETCHALL","C");
            addSample(con,"Computer Networks","Which protocol is used to browse secure websites?","FTP","HTTPS","SMTP","Telnet","B");
        }
    }

    private static void addSample(Connection con,String cat,String q,String a,String b,String c,String d,String answer)throws SQLException{
        try(PreparedStatement check=con.prepareStatement("SELECT COUNT(*) FROM questions WHERE question_text=?");
            PreparedStatement insert=con.prepareStatement("""
              INSERT INTO questions(category_id,question_text,option_a,option_b,option_c,option_d,correct_option)
              SELECT category_id,?,?,?,?,?,? FROM categories WHERE category_name=?""")){
            check.setString(1,q);try(ResultSet rs=check.executeQuery()){if(rs.next()&&rs.getInt(1)>0)return;}
            insert.setString(1,q);insert.setString(2,a);insert.setString(3,b);insert.setString(4,c);
            insert.setString(5,d);insert.setString(6,answer);insert.setString(7,cat);insert.executeUpdate();
        }
    }
}