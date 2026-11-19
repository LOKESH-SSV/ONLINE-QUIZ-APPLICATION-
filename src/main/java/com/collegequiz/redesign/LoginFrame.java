package com.collegequiz.redesign;

import com.collegequiz.redesign.model.User;
import com.collegequiz.redesign.service.AuthService;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class LoginFrame extends Frame {
    private final TextField email = new TextField(25);
    private final TextField password = new TextField(25);
    private final AuthService auth = new AuthService();

    public LoginFrame() {
        super("QuizPilot | Sign in");
        setSize(520, 410);
        setLocationRelativeTo(null);
        setBackground(new Color(244, 247, 252));
        setLayout(new GridBagLayout());
        password.setEchoChar('•');

        Panel card = new Panel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(9, 12, 9, 12);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        Label heading = new Label("QUIZPILOT", Label.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 27));
        heading.setForeground(new Color(36, 78, 145));
        card.add(heading, c);
        c.gridy++;
        Label subtitle = new Label("Sign in to continue learning", Label.CENTER);
        subtitle.setForeground(new Color(95, 105, 120));
        card.add(subtitle, c);
        c.gridwidth = 1;
        addRow(card, c, 2, "Email address", email);
        addRow(card, c, 3, "Password", password);

        Button signIn = new Button("Sign in");
        signIn.setFont(new Font("Arial", Font.BOLD, 14));
        Button create = new Button("Create account");
        Panel actions = new Panel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        actions.setBackground(Color.WHITE);
        actions.add(signIn); actions.add(create);
        c.gridx = 0; c.gridy = 4; c.gridwidth = 2;
        card.add(actions, c);
        add(card);

        signIn.addActionListener(event -> login());
        password.addActionListener(event -> login());
        create.addActionListener(event -> { new RegisterFrame(); dispose(); });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { dispose(); }
        });
        setVisible(true);
    }

    private void addRow(Panel panel, GridBagConstraints c, int y, String label, Component field) {
        c.gridx = 0; c.gridy = y; panel.add(new Label(label), c);
        c.gridx = 1; panel.add(field, c);
    }

    private void login() {
        try {
            User user = auth.login(email.getText(), password.getText());
            new RoleDashboardFrame(user);
            dispose();
        } catch (Exception exception) {
            showMessage(exception.getMessage());
        }
    }

    private void showMessage(String message) {
        new MessageDialog(this, message == null ? "Unable to sign in." : message);
    }
}