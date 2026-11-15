package com.collegequiz;
import java.awt.EventQueue;
import java.sql.SQLException;
public class Main {
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try { DB.initialize(); new AuthFrame(); }
            catch(SQLException ex) {
                System.err.println("Could not initialize SQLite database: "+ex.getMessage());
                ex.printStackTrace();
            }
        });
    }
}
