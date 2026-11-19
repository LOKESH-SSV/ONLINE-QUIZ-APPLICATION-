package com.collegequiz.redesign;

import com.collegequiz.redesign.service.AuthService;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class RegisterFrame extends Frame {
    private final TextField fullName = new TextField(24);
    private final TextField email = new TextField(24);
    private final TextField password = new TextField(24);
    private final TextField confirm = new TextField(24);
    private final Choice role = new Choice();
    private final AuthService auth = new AuthService();

    public RegisterFrame() {
        super("QuizPilot | Create account");
        setSize(540, 520);
        setLocationRelativeTo(null);
        setBackground(new Color(244, 247, 252));
        setLayout(new GridBagLayout());
        password.setEchoChar('•'); confirm.setEchoChar('•');
        role.add("Student"); role.add("Teacher");

        Panel card = new Panel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 12, 7, 12); c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        Label heading = new Label("CREATE YOUR ACCOUNT", Label.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 23));
        heading.setForeground(new Color(36, 78, 145));
        card.add(heading, c);
        c.gridwidth = 1;
        addRow(card, c, 1, "Full name", fullName);
        addRow(card, c, 2, "Email address", email);
        addRow(card, c, 3, "Password (8+ characters)", password);
        addRow(card, c, 4, "Confirm password", confirm);
        addRow(card, c, 5, "Account type", role);

        Button register = new Button("Create account");
        Button back = new Button("Back to sign in");
        Panel actions = new Panel(new FlowLayout());
        actions.setBackground(Color.WHITE); actions.add(register); actions.add(back);
        c.gridx = 0; c.gridy = 6; c.gridwidth = 2; card.add(actions, c);
        add(card);

        register.addActionListener(event -> register());
        back.addActionListener(event -> { new LoginFrame(); dispose(); });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { dispose(); }
        });
        setVisible(true);
    }

    private void addRow(Panel panel, GridBagConstraints c, int y, String label, Component field) {
        c.gridx = 0; c.gridy = y; panel.add(new Label(label), c);
        c.gridx = 1; panel.add(field, c);
    }

    private void register() {
        try {
            auth.register(fullName.getText(), email.getText(), password.getText(),
                    confirm.getText(), "Teacher".equals(role.getSelectedItem()) ? "TEACHER" : "STUDENT");
            new MessageDialog(this, "Account created successfully. Please sign in.");
            new LoginFrame();
            dispose();
        } catch (Exception exception) {
            new MessageDialog(this, exception.getMessage() == null
                    ? "Unable to create account." : exception.getMessage());
        }
    }
}