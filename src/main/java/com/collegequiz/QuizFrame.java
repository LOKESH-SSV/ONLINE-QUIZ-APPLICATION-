package com.collegequiz;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class QuizFrame extends Frame {
    private final int userId, quizId;
    private final String userName, role, category;
    private final List<Question> questions;
    private final Map<Integer, String> answers = new HashMap<>();
    private final Set<Integer> review = new HashSet<>();
    private int index = 0, secondsLeft, attemptId = -1;
    private final int totalSeconds;
    private final boolean negativeEnabled;
    private final double negativeMarks;
    private final Label timer = new Label();
    private final Label questionLabel = new Label("", Label.LEFT);
    private final Label progress = new Label();
    private final CheckboxGroup group = new CheckboxGroup();
    private final Checkbox[] options = new Checkbox[4];
    private final Button previous = new Button("Previous");
    private final Button next = new Button("Next");
    private final Button reviewButton = new Button("Mark for Review");
    private final Button submit = new Button("Submit Quiz");
    private final Panel palette = new Panel(new GridLayout(0, 4, 4, 4));
    private final Button[] questionButtons;
    private volatile boolean running = true;
    private boolean loadingQuestion = false;

    public QuizFrame(int id, String name, String role, String cat, List<Question> qs) {
        this(id, name, role, cat, qs, 10, -1, false, 0);
    }

    public QuizFrame(int id, String name, String role, String cat, List<Question> qs,
                     int minutes, int quizId) {
        this(id, name, role, cat, qs, minutes, quizId, false, 0);
    }

    public QuizFrame(int id, String name, String role, String cat, List<Question> qs,
                     int minutes, int quizId, boolean negativeEnabled, double negativeMarks) {
        super("Quiz - " + cat);
        userId = id; userName = name; this.role = role; category = cat;
        questions = qs; this.quizId = quizId;
        this.negativeEnabled = negativeEnabled; this.negativeMarks = negativeEnabled ? negativeMarks : 0;
        totalSeconds = Math.max(60, minutes * 60); secondsLeft = totalSeconds;
        questionButtons = new Button[questions.size()];

        setSize(900, 590); setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        Panel top = new Panel(new BorderLayout());
        top.add(new Label("Subject: " + cat + "     Total questions: " + qs.size()), BorderLayout.WEST);
        timer.setFont(new Font("Arial", Font.BOLD, 17));
        timer.setForeground(new Color(180, 45, 45));
        updateTimer(); top.add(timer, BorderLayout.EAST); add(top, BorderLayout.NORTH);

        Panel center = new Panel(new BorderLayout(8, 8));
        Panel questionArea = new Panel(new GridLayout(5, 1, 5, 8));
        questionLabel.setFont(new Font("Arial", Font.BOLD, 17));
        questionArea.add(questionLabel);
        for (int i = 0; i < 4; i++) {
            options[i] = new Checkbox("", group, false);
            options[i].setFont(new Font("Arial", Font.PLAIN, 15));
            questionArea.add(options[i]);
            options[i].addItemListener(e -> {
                if (!loadingQuestion && (options[0].getState() || options[1].getState()
                        || options[2].getState() || options[3].getState())) {
                    saveCurrent();
                    refreshPalette();
                }
            });
        }
        center.add(questionArea, BorderLayout.CENTER);
        Panel nav = new Panel(new FlowLayout(FlowLayout.LEFT));
        nav.add(previous); nav.add(next); nav.add(reviewButton);
        center.add(nav, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        Panel east = new Panel(new BorderLayout(5, 5));
        east.add(new Label("Question palette"), BorderLayout.NORTH);
        palette.setPreferredSize(new Dimension(190, 300));
        for (int i = 0; i < questions.size(); i++) {
            final int target = i;
            questionButtons[i] = new Button(String.valueOf(i + 1));
            questionButtons[i].addActionListener(e -> {
                saveCurrent(); index = target; showQuestion();
            });
            palette.add(questionButtons[i]);
        }
        east.add(palette, BorderLayout.CENTER);
        progress.setText("Answered: 0 | Review: 0");
        east.add(progress, BorderLayout.SOUTH);
        add(east, BorderLayout.EAST);

        Panel bottom = new Panel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(submit); add(bottom, BorderLayout.SOUTH);
        previous.addActionListener(e -> { saveCurrent(); if (index > 0) { index--; showQuestion(); } });
        next.addActionListener(e -> { saveCurrent(); if (index < questions.size() - 1) { index++; showQuestion(); } });
        reviewButton.addActionListener(e -> toggleReview());
        submit.addActionListener(e -> confirmFinish());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { confirmFinish(); }
        });

        if (quizId > 0 && !createAttempt()) { dispose(); return; }
        showQuestion();
        setVisible(true);
        Thread countdown = new Thread(() -> {
            while (running && secondsLeft > 0) {
                try { Thread.sleep(1000); } catch (InterruptedException ex) { return; }
                secondsLeft--;
                EventQueue.invokeLater(this::updateTimer);
            }
            if (running) EventQueue.invokeLater(() -> finish(true));
        }, "quiz-countdown");
        countdown.setDaemon(true); countdown.start();
    }

    private boolean createAttempt() {
        String sql = "INSERT INTO attempts(quiz_id,user_id,total_questions,correct_answers,score_percent,time_taken_seconds,status,started_at) VALUES(?,?,?,0,0,0,'IN_PROGRESS',CURRENT_TIMESTAMP)";
        try (Connection c = DB.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, quizId); p.setInt(2, userId); p.setInt(3, questions.size());
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Could not start the quiz attempt.");
                attemptId = keys.getInt(1);
            }
            return true;
        } catch (SQLException ex) {
            Msg.show(this, "Could not start quiz: " + ex.getMessage());
            return false;
        }
    }

    private void updateTimer() {
        timer.setText(String.format("Time remaining: %02d:%02d", secondsLeft / 60, secondsLeft % 60));
    }

    private void showQuestion() {
        Question q = questions.get(index);
        questionLabel.setText("Question " + (index + 1) + " of " + questions.size() + ": " + q.text);
        String[] values = {q.a, q.b, q.c, q.d};
        String saved = answers.get(q.id);
        loadingQuestion = true;
        group.setSelectedCheckbox(null);
        for (int i = 0; i < 4; i++) {
            options[i].setLabel((char) ('A' + i) + ". " + values[i]);
            options[i].setState(saved != null && saved.equals(String.valueOf((char) ('A' + i))));
        }
        loadingQuestion = false;
        previous.setEnabled(index > 0);
        next.setEnabled(index < questions.size() - 1);
        reviewButton.setLabel(review.contains(q.id) ? "Remove Review Mark" : "Mark for Review");
        refreshPalette();
    }

    private void saveCurrent() {
        Question q = questions.get(index);
        String selected = null;
        for (int i = 0; i < 4; i++)
            if (options[i].getState()) selected = String.valueOf((char) ('A' + i));
        if (selected == null) answers.remove(q.id); else answers.put(q.id, selected);
        if (attemptId > 0) persistAnswer(q.id, selected, review.contains(q.id));
    }

    private void persistAnswer(int questionId, String selected, boolean marked) {
        String sql = """
            INSERT INTO student_answers(attempt_id,question_id,selected_option,is_correct,marked_for_review)
            VALUES(?,?,?,0,?)
            ON CONFLICT(attempt_id,question_id) DO UPDATE SET
              selected_option=excluded.selected_option, marked_for_review=excluded.marked_for_review
            """;
        try (Connection c = DB.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, attemptId); p.setInt(2, questionId); p.setString(3, selected);
            p.setInt(4, marked ? 1 : 0); p.executeUpdate();
        } catch (SQLException ex) {
            Msg.show(this, "Answer could not be saved: " + ex.getMessage());
        }
    }

    private void toggleReview() {
        int qid = questions.get(index).id;
        if (review.contains(qid)) review.remove(qid); else review.add(qid);
        saveCurrent(); refreshPalette();
        reviewButton.setLabel(review.contains(qid) ? "Remove Review Mark" : "Mark for Review");
    }

    private void refreshPalette() {
        int answered = 0;
        for (int i = 0; i < questions.size(); i++) {
            int qid = questions.get(i).id;
            boolean hasAnswer = answers.containsKey(qid);
            if (hasAnswer) answered++;
            String marker = (i == index ? "> " : "") + (i + 1)
                    + (review.contains(qid) ? " *" : hasAnswer ? " ✓" : "");
            questionButtons[i].setLabel(marker);
        }
        progress.setText("Answered: " + answered + " | Review: " + review.size());
    }

    private void confirmFinish() {
        if (!running) return;
        saveCurrent();
        if (Msg.confirm(this, "Submit this quiz now? Unanswered questions will receive zero marks.")) finish(false);
    }

    private void finish(boolean timedOut) {
        if (!running) return;
        running = false; saveCurrent();
        int correct = 0, incorrect = 0, unanswered = 0;
        for (Question q : questions) {
            String answer = answers.get(q.id);
            if (answer == null) unanswered++;
            else if (q.correct.equals(answer)) correct++;
            else incorrect++;
        }
        double penalty = incorrect * negativeMarks;
        double rawScore = correct - penalty;
        double percent = 100.0 * rawScore / questions.size();
        int elapsed = totalSeconds - secondsLeft;
        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement p = con.prepareStatement("""
                INSERT INTO results(user_id,category_id,total_questions,correct_answers,score_percent,time_taken_seconds)
                SELECT ?,category_id,?,?,?,? FROM categories WHERE category_name=?""")) {
                p.setInt(1, userId); p.setInt(2, questions.size()); p.setInt(3, correct);
                p.setDouble(4, percent); p.setInt(5, elapsed); p.setString(6, category); p.executeUpdate();
            }
            if (attemptId > 0) {
                try (PreparedStatement p = con.prepareStatement("""
                    UPDATE attempts SET correct_answers=?,incorrect_answers=?,unanswered_answers=?,
                    raw_score=?,negative_marks_applied=?,score_percent=?,time_taken_seconds=?,
                    status='SUBMITTED',submitted_at=CURRENT_TIMESTAMP WHERE attempt_id=?""")) {
                    p.setInt(1, correct); p.setInt(2, incorrect); p.setInt(3, unanswered);
                    p.setDouble(4, rawScore); p.setDouble(5, penalty); p.setDouble(6, percent);
                    p.setInt(7, elapsed); p.setInt(8, attemptId); p.executeUpdate();
                }
                try (PreparedStatement p = con.prepareStatement("""
                    INSERT INTO student_answers(attempt_id,question_id,selected_option,is_correct,marked_for_review)
                    VALUES(?,?,?,?,?)
                    ON CONFLICT(attempt_id,question_id) DO UPDATE SET
                    selected_option=excluded.selected_option,is_correct=excluded.is_correct,
                    marked_for_review=excluded.marked_for_review""")) {
                    for (Question q : questions) {
                        String selected = answers.get(q.id);
                        p.setInt(1, attemptId); p.setInt(2, q.id); p.setString(3, selected);
                        p.setInt(4, q.correct.equals(selected) ? 1 : 0);
                        p.setInt(5, review.contains(q.id) ? 1 : 0); p.addBatch();
                    }
                    p.executeBatch();
                }
            }
            con.commit();
            String result = (timedOut ? "Time is over. Your quiz was auto-submitted.\n\n" : "QUIZ COMPLETED!\n\n")
                    + String.format("Correct: %d\nWrong: %d\nUnanswered: %d\nNegative marks: %.2f\nFinal score: %.2f / %d\nPercentage: %.2f%%\nTime taken: %d seconds",
                    correct, incorrect, unanswered, penalty, rawScore, questions.size(), percent, elapsed);
            Msg.show(this, result);
            dispose(); new DashboardFrame(userId, userName, role);
        } catch (SQLException ex) {
            Msg.show(this, "Could not save result: " + ex.getMessage());
            dispose();
        }
    }
}
