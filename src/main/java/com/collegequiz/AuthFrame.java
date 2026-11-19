package com.collegequiz;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class AuthFrame extends Frame {
    private final TextField name = new TextField(22);
    private final TextField email = new TextField(22);
    private final TextField password = new TextField(22);
    private final Choice accountType = new Choice();
    private final TextField teacherCode = new TextField(22);
    private static final String TEACHER_CODE =
            System.getProperty("quiz.teacherCode", "QUIZ-TEACHER-2026");

    public AuthFrame() {
        super("Online Quiz Application - Login / Register");
        setSize(500, 390);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        setBackground(new Color(242, 246, 255));
        password.setEchoChar('*');
        accountType.add("Student");
        accountType.add("Teacher");

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        Label title = new Label("ONLINE QUIZ APPLICATION", Label.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(new Color(35, 65, 125));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        add(title, c);
        c.gridwidth = 1;

        row(c, 1, "Full name (for registration)", name);
        row(c, 2, "Email", email);
        row(c, 3, "Password", password);
        row(c, 4, "Register as", accountType);
        row(c, 5, "Teacher invitation code", teacherCode);
        teacherCode.setEnabled(false);
        accountType.addItemListener(e ->
                teacherCode.setEnabled("Teacher".equals(accountType.getSelectedItem())));

        Button login = new Button("Login");
        Button register = new Button("Create Account");
        Panel buttons = new Panel();
        buttons.add(login);
        buttons.add(register);
        c.gridx = 0; c.gridy = 6; c.gridwidth = 2;
        add(buttons, c);

        Label note = new Label("Choose Student or Teacher when creating an account.", Label.CENTER);
        c.gridy = 7;
        add(note, c);

        login.addActionListener(e -> login());
        register.addActionListener(e -> register());
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });
        setVisible(true);
    }

    private void row(GridBagConstraints c, int y, String label, Component field) {
        c.gridx = 0; c.gridy = y;
        add(new Label(label), c);
        c.gridx = 1;
        add(field, c);
    }

    private void login() {
        String sql = "SELECT user_id,full_name,password_hash,role FROM users WHERE email=?";
        try (Connection con = DB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.getText().trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordUtil.verify(password.getText(), rs.getString("password_hash"))) {
                    new DashboardFrame(rs.getInt("user_id"), rs.getString("full_name"), rs.getString("role"));
                    dispose();
                } else {
                    Msg.show(this, "Invalid email or password.");
                }
            }
        } catch (SQLException ex) {
            Msg.error(this, ex);
        }
    }

    private void register() {
        String full = name.getText().trim();
        String mail = email.getText().trim();
        String pass = password.getText();
        if (full.isEmpty() || mail.isEmpty() || pass.length() < 6) {
            Msg.show(this, "Enter your name, email and a password of at least 6 characters.");
            return;
        }

        // ADMIN is the internal database role used by the teacher question-management panel.
        boolean isTeacher = accountType.getSelectedItem().equals("Teacher");
        if (isTeacher && !TEACHER_CODE.equals(teacherCode.getText().trim())) {
            Msg.show(this, "A valid teacher invitation code is required.");
            return;
        }
        String role = isTeacher ? "ADMIN" : "STUDENT";
        String sql = "INSERT INTO users(full_name,email,password_hash,role) VALUES(?,?,?,?)";
        try (Connection con = DB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, full);
            ps.setString(2, mail);
            ps.setString(3, PasswordUtil.hash(pass));
            ps.setString(4, role);
            ps.executeUpdate();
            Msg.show(this, "Account created successfully as " + accountType.getSelectedItem() + ". You can now log in.");
            name.setText("");
            email.setText("");
            password.setText("");
            teacherCode.setText("");
            accountType.select("Student");
        } catch (SQLException ex) {
            Msg.error(this, ex);
        }
    }
}
