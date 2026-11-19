package com.collegequiz.redesign;

import java.awt.EventQueue;
import java.io.IOException;
import java.sql.SQLException;

public final class QuizRedesignBootstrap {
    private QuizRedesignBootstrap() { }

    public static void initialize() throws SQLException, IOException {
        DatabaseMigrator.migrate();
        EventQueue.invokeLater(LoginFrame::new);
    }
}