package com.collegequiz;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class QuizManagementFrame extends Frame {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final int teacherId;
    private final TextField title = new TextField(24), duration = new TextField("10", 6),
            count = new TextField("5", 6), code = new TextField(12),
            startsAt = new TextField(16), endsAt = new TextField(16),
            penalty = new TextField("0.25", 6);
    private final Choice category = new Choice();
    private final Checkbox negative = new Checkbox("Enable negative marking", false);
    private final Checkbox useBank = new Checkbox("Choose questions from existing question bank", false);
    private final TextArea list = new TextArea(12, 65);

    public QuizManagementFrame(int teacherId) {
        super("Teacher - Quiz Management"); this.teacherId = teacherId;
        setSize(760, 680); setLocationRelativeTo(null); setLayout(new BorderLayout(8, 8));
        Panel form = new Panel(new GridLayout(0, 2, 7, 5));
        addField(form, "Quiz title", title); addField(form, "Subject", category);
        addField(form, "Duration (minutes)", duration); addField(form, "Number of questions", count);
        addField(form, "Access code (optional)", code);
        addField(form, "Start time (yyyy-MM-dd HH:mm, optional)", startsAt);
        addField(form, "End time (yyyy-MM-dd HH:mm, optional)", endsAt);
        addField(form, "Negative marking", negative);
        addField(form, "Penalty per wrong answer", penalty);
        addField(form, "Question source", useBank);
        Button create = new Button("Create Quiz and Add Questions");
        Button refresh = new Button("Refresh"); Button closeQuiz = new Button("Close Selected Quiz");
        Button reports = new Button("Export Teacher Report");
        Panel actions = new Panel(); actions.add(create); actions.add(refresh); actions.add(closeQuiz); actions.add(reports);
        Panel north = new Panel(new BorderLayout()); north.add(form, BorderLayout.CENTER); north.add(actions, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH); list.setEditable(false); add(list, BorderLayout.CENTER);
        penalty.setEnabled(false); negative.addItemListener(e -> penalty.setEnabled(negative.getState()));
        create.addActionListener(e -> createQuiz()); refresh.addActionListener(e -> loadQuizzes());
        closeQuiz.addActionListener(e -> closeSelected());
        reports.addActionListener(e -> new AnalyticsFrame(teacherId));
        addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { dispose(); } });
        loadCategories(); loadQuizzes(); setVisible(true);
    }

    private void addField(Panel p, String label, Component field) { p.add(new Label(label)); p.add(field); }

    private void loadCategories() {
        try (Connection c = DB.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT category_name FROM categories ORDER BY category_name")) {
            while (r.next()) category.add(r.getString(1));
        } catch (SQLException e) { Msg.error(this, e); }
    }

    private void createQuiz() {
        String quizTitle = title.getText().trim();
        int minutes, questionCount;
        try {
            minutes = Integer.parseInt(duration.getText().trim());
            questionCount = Integer.parseInt(count.getText().trim());
        } catch (NumberFormatException e) {
            Msg.show(this, "Duration and question count must be valid numbers."); return;
        }
        if (quizTitle.isEmpty() || minutes < 1 || questionCount < 1 || questionCount > 100) {
            Msg.show(this, "Enter a title, positive duration and 1 to 100 questions."); return;
        }
        double wrongPenalty = 0;
        if (negative.getState()) {
            try { wrongPenalty = Double.parseDouble(penalty.getText().trim()); }
            catch (NumberFormatException e) { Msg.show(this, "Enter a valid negative-mark value."); return; }
            if (wrongPenalty <= 0 || wrongPenalty > 10) {
                Msg.show(this, "Penalty must be greater than 0 and no more than 10."); return;
            }
        }
        String start = normalizeDate(startsAt.getText().trim());
        String end = normalizeDate(endsAt.getText().trim());
        if (start == null && !startsAt.getText().trim().isEmpty()
                || end == null && !endsAt.getText().trim().isEmpty()) {
            Msg.show(this, "Use date format yyyy-MM-dd HH:mm, or leave the field blank."); return;
        }
        if (start != null && end != null && end.compareTo(start) <= 0) {
            Msg.show(this, "End time must be later than start time."); return;
        }
        String access = code.getText().trim();
        if (access.isEmpty()) access = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String subject = category.getSelectedItem();
        try (Connection c = DB.getConnection()) {
            c.setAutoCommit(false);
            try {
                int categoryId;
                try (PreparedStatement p = c.prepareStatement("SELECT category_id FROM categories WHERE category_name=?")) {
                    p.setString(1, subject);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new SQLException("Choose a valid subject.");
                        categoryId = r.getInt(1);
                    }
                }
                int quizId;
                String insert = """
                    INSERT INTO quizzes(teacher_id,title,category_id,duration_minutes,question_count,
                    access_code,status,negative_marking_enabled,negative_marks,starts_at,ends_at)
                    VALUES(?,?,?,?,?,?,'DRAFT',?,?,?,?)""";
                try (PreparedStatement p = c.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                    p.setInt(1, teacherId); p.setString(2, quizTitle); p.setInt(3, categoryId);
                    p.setInt(4, minutes); p.setInt(5, questionCount); p.setString(6, access);
                    p.setInt(7, negative.getState() ? 1 : 0); p.setDouble(8, wrongPenalty);
                    p.setString(9, start); p.setString(10, end); p.executeUpdate();
                    try (ResultSet r = p.getGeneratedKeys()) {
                        if (!r.next()) throw new SQLException("Could not create quiz.");
                        quizId = r.getInt(1);
                    }
                }
                if (useBank.getState()) {
                    int[] selected = chooseBankQuestions(categoryId, questionCount);
                    if (selected == null) { c.rollback(); return; }
                    try (PreparedStatement p = c.prepareStatement(
                            "INSERT INTO quiz_questions(quiz_id,question_id) VALUES(?,?)")) {
                        for (int qid : selected) { p.setInt(1, quizId); p.setInt(2, qid); p.addBatch(); }
                        p.executeBatch();
                    }
                } else {
                    for (int i = 1; i <= questionCount; i++) {
                        String[] data = showQuestionDialog(i, questionCount);
                        if (data == null) {
                            c.rollback(); Msg.show(this, "Quiz creation cancelled. No incomplete quiz was saved."); return;
                        }
                        int qid;
                        try (PreparedStatement p = c.prepareStatement("""
                            INSERT INTO questions(category_id,question_text,option_a,option_b,option_c,option_d,
                            correct_option,difficulty,topic,explanation) VALUES(?,?,?,?,?,?,?,?,?,?)""",
                            Statement.RETURN_GENERATED_KEYS)) {
                            p.setInt(1, categoryId);
                            for (int j = 0; j < 6; j++) p.setString(j + 2, data[j]);
                            p.setString(8, data[6]); p.setString(9, data[7]); p.setString(10, data[8]);
                            p.executeUpdate();
                            try (ResultSet r = p.getGeneratedKeys()) {
                                if (!r.next()) throw new SQLException("Could not save question " + i + ".");
                                qid = r.getInt(1);
                            }
                        }
                        try (PreparedStatement p = c.prepareStatement(
                                "INSERT INTO quiz_questions(quiz_id,question_id) VALUES(?,?)")) {
                            p.setInt(1, quizId); p.setInt(2, qid); p.executeUpdate();
                        }
                    }
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE quizzes SET status='PUBLISHED' WHERE quiz_id=?")) {
                    p.setInt(1, quizId); p.executeUpdate();
                }
                c.commit();
                Msg.show(this, "Quiz published!\nQuestions: " + questionCount
                        + "\nNegative marking: " + (negative.getState() ? wrongPenalty + " per wrong answer" : "Disabled")
                        + "\nAccess code: " + access);
                title.setText(""); code.setText(""); startsAt.setText(""); endsAt.setText(""); loadQuizzes();
            } catch (Exception ex) { c.rollback(); throw ex; }
        } catch (SQLException e) { Msg.error(this, e); }
    }

    private String normalizeDate(String value) {
        if (value.isEmpty()) return null;
        try { return LocalDateTime.parse(value, DATE_FORMAT).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
        catch (Exception e) { return null; }
    }

    private int[] chooseBankQuestions(int categoryId, int expected) throws SQLException {
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        java.util.List<String> labels = new java.util.ArrayList<>();
        try (Connection c = DB.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT question_id,question_text,difficulty,topic FROM questions WHERE category_id=? ORDER BY question_id")) {
            p.setInt(1, categoryId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) { ids.add(r.getInt(1)); labels.add(r.getInt(1) + " | " + r.getString(2)
                        + " [" + r.getString(3) + "/" + r.getString(4) + "]"); }
            }
        }
        if (ids.size() < expected) {
            Msg.show(this, "Question bank has only " + ids.size() + " questions. Add more or enter new questions.");
            return null;
        }
        Dialog d = new Dialog(this, "Choose " + expected + " questions", true);
        d.setLayout(new BorderLayout(8, 8)); d.setSize(620, 430); d.setLocationRelativeTo(this);
        java.awt.List choices = new java.awt.List(14, true);
        for (String label : labels) choices.add(label);
        d.add(new Label("Select exactly " + expected + " questions (Ctrl/Command-click for multiple)."), BorderLayout.NORTH);
        d.add(choices, BorderLayout.CENTER); final int[][] result = {null};
        Panel buttons = new Panel(); Button ok = new Button("Use Selected Questions"), cancel = new Button("Cancel");
        buttons.add(ok); buttons.add(cancel); d.add(buttons, BorderLayout.SOUTH);
        ok.addActionListener(e -> {
            int[] selected = choices.getSelectedIndexes();
            if (selected.length != expected) { Msg.show(this, "Select exactly " + expected + " questions."); return; }
            result[0] = new int[selected.length];
            for (int i = 0; i < selected.length; i++) result[0][i] = ids.get(selected[i]);
            d.dispose();
        });
        cancel.addActionListener(e -> d.dispose());
        d.addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { d.dispose(); } });
        d.setVisible(true); return result[0];
    }

    // question, A, B, C, D, correct option, difficulty, topic, explanation
    private String[] showQuestionDialog(int number, int total) {
        Dialog d = new Dialog(this, "Enter Question " + number + " of " + total, true);
        d.setLayout(new BorderLayout(8, 8)); d.setSize(560, 500); d.setLocationRelativeTo(this);
        Panel fields = new Panel(new GridLayout(0, 2, 6, 6));
        TextField q = new TextField(30);
        TextField[] o = {new TextField(24), new TextField(24), new TextField(24), new TextField(24)};
        Choice answer = new Choice(); for (String x : new String[]{"A", "B", "C", "D"}) answer.add(x);
        Choice difficulty = new Choice(); for (String x : new String[]{"EASY", "MEDIUM", "HARD"}) difficulty.add(x);
        TextField topic = new TextField(20); TextField explanation = new TextField(25);
        fields.add(new Label("Question")); fields.add(q);
        for (int i = 0; i < 4; i++) { fields.add(new Label("Option " + (char) ('A' + i))); fields.add(o[i]); }
        fields.add(new Label("Correct option")); fields.add(answer);
        fields.add(new Label("Difficulty")); fields.add(difficulty);
        fields.add(new Label("Topic")); fields.add(topic);
        fields.add(new Label("Answer explanation (optional)")); fields.add(explanation);
        d.add(new Label("New questions are also saved in the reusable question bank."), BorderLayout.NORTH);
        d.add(fields, BorderLayout.CENTER);
        final String[][] result = {null}; Panel buttons = new Panel();
        Button save = new Button(number == total ? "Save and Publish" : "Save and Continue");
        Button cancel = new Button("Cancel Quiz"); buttons.add(save); buttons.add(cancel); d.add(buttons, BorderLayout.SOUTH);
        save.addActionListener(e -> {
            if (q.getText().trim().isEmpty()) { Msg.show(this, "Enter question text."); return; }
            for (TextField f : o) if (f.getText().trim().isEmpty()) { Msg.show(this, "Fill all four options."); return; }
            result[0] = new String[]{q.getText().trim(), o[0].getText().trim(), o[1].getText().trim(),
                    o[2].getText().trim(), o[3].getText().trim(), answer.getSelectedItem(),
                    difficulty.getSelectedItem(), topic.getText().trim(), explanation.getText().trim()};
            d.dispose();
        });
        cancel.addActionListener(e -> d.dispose());
        d.addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { d.dispose(); } });
        d.setVisible(true); return result[0];
    }

    private void loadQuizzes() {
        StringBuilder b = new StringBuilder("YOUR QUIZZES\n\nID | TITLE | SUBJECT | QUESTIONS | MINUTES | CODE | STATUS | NEGATIVE MARKING | SCHEDULE\n");
        try (Connection c = DB.getConnection(); PreparedStatement p = c.prepareStatement("""
            SELECT q.quiz_id,q.title,c.category_name,q.question_count,q.duration_minutes,q.access_code,
            q.status,q.negative_marking_enabled,q.negative_marks,COALESCE(q.starts_at,'Any time'),
            COALESCE(q.ends_at,'No end time') FROM quizzes q JOIN categories c ON c.category_id=q.category_id
            WHERE q.teacher_id=? ORDER BY q.quiz_id DESC""")) {
            p.setInt(1, teacherId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) b.append(r.getInt(1)).append(" | ").append(r.getString(2)).append(" | ")
                        .append(r.getString(3)).append(" | ").append(r.getInt(4)).append(" | ").append(r.getInt(5))
                        .append(" | ").append(r.getString(6)).append(" | ").append(r.getString(7)).append(" | ")
                        .append(r.getInt(8) == 1 ? r.getDouble(9) : "Off").append(" | ")
                        .append(r.getString(10)).append(" - ").append(r.getString(11)).append("\n");
            }
            list.setText(b.toString());
        } catch (SQLException e) { Msg.error(this, e); }
    }

    private void closeSelected() {
        String input = Msg.prompt(this, "Enter the quiz ID to close:");
        if (input == null) return;
        try {
            int id = Integer.parseInt(input.trim());
            try (Connection c = DB.getConnection(); PreparedStatement p = c.prepareStatement(
                    "UPDATE quizzes SET status='CLOSED' WHERE quiz_id=? AND teacher_id=?")) {
                p.setInt(1, id); p.setInt(2, teacherId); int n = p.executeUpdate();
                Msg.show(this, n == 1 ? "Quiz closed." : "Quiz ID not found."); loadQuizzes();
            }
        } catch (Exception e) { Msg.show(this, "Enter a valid quiz ID."); }
    }
}
