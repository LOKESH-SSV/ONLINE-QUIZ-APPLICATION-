package com.collegequiz.redesign;

import com.collegequiz.redesign.model.User;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class RoleDashboardFrame extends Frame {
    public RoleDashboardFrame(User user) {
        super("QuizPilot | " + (user.isTeacher() ? "Teacher" : "Student") + " dashboard");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setBackground(new Color(244, 247, 252));
        setLayout(new BorderLayout(12, 12));

        Panel header = new Panel(new BorderLayout());
        header.setBackground(new Color(36, 78, 145));
        Label brand = new Label("  QUIZPILOT");
        brand.setFont(new Font("Arial", Font.BOLD, 22));
        brand.setForeground(Color.WHITE);
        Label identity = new Label(user.fullName() + "  |  "
                + (user.isTeacher() ? "TEACHER  " : "STUDENT  "));
        identity.setForeground(Color.WHITE);
        header.add(brand, BorderLayout.WEST);
        header.add(identity, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        Panel content = new Panel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 16, 10, 16);
        Label welcome = new Label("Welcome, " + user.fullName(), Label.CENTER);
        welcome.setFont(new Font("Arial", Font.BOLD, 25));
        c.gridx = 0; c.gridy = 0; content.add(welcome, c);
        Label description = new Label(user.isTeacher()
                ? "Teacher workspace — create quizzes and review student performance."
                : "Student workspace — join quizzes and track your learning.");
        c.gridy = 1; content.add(description, c);
        Label status = new Label("Your account is ready. Quiz workspace modules are being connected.");
        status.setForeground(new Color(90, 100, 115));
        c.gridy = 2; content.add(status, c);
        add(content, BorderLayout.CENTER);

        Button logout = new Button("Sign out");
        Panel footer = new Panel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(logout); add(footer, BorderLayout.SOUTH);
        logout.addActionListener(event -> { new LoginFrame(); dispose(); });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { dispose(); }
        });
        setVisible(true);
    }
}