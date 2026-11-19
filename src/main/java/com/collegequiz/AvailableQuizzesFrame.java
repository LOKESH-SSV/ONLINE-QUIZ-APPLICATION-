package com.collegequiz;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AvailableQuizzesFrame extends Frame {
    private final int userId;
    private final String name, role;
    private final TextArea quizzes = new TextArea(16, 70);
    private final TextField accessCode = new TextField(12);

    public AvailableQuizzesFrame(int userId, String name, String role) {
        super("Available Published Quizzes"); this.userId=userId; this.name=name; this.role=role;
        setSize(780,460); setLocationRelativeTo(null); setLayout(new BorderLayout(8,8));
        quizzes.setEditable(false); add(quizzes,BorderLayout.CENTER);
        Panel bottom=new Panel();bottom.add(new Label("Quiz access code:"));bottom.add(accessCode);
        Button join=new Button("Join Quiz"),refresh=new Button("Refresh");bottom.add(join);bottom.add(refresh);
        add(bottom,BorderLayout.SOUTH);
        join.addActionListener(e->joinQuiz());refresh.addActionListener(e->load());
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){dispose();}});
        load();setVisible(true);
    }

    private void load() {
        StringBuilder b=new StringBuilder("PUBLISHED QUIZZES\n\nID | TITLE | SUBJECT | QUESTIONS | TIME | SCHEDULE | NEGATIVE MARKING\n");
        String sql="""
            SELECT q.quiz_id,q.title,c.category_name,q.question_count,q.duration_minutes,
            COALESCE(q.starts_at,'Any time'),COALESCE(q.ends_at,'No end time'),
            q.negative_marking_enabled,q.negative_marks
            FROM quizzes q JOIN categories c ON c.category_id=q.category_id
            WHERE q.status='PUBLISHED' ORDER BY q.quiz_id DESC""";
        try(Connection c=DB.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){
            while(r.next()) b.append(r.getInt(1)).append(" | ").append(r.getString(2)).append(" | ")
                    .append(r.getString(3)).append(" | ").append(r.getInt(4)).append(" | ")
                    .append(r.getInt(5)).append(" min | ").append(r.getString(6)).append(" - ")
                    .append(r.getString(7)).append(" | ")
                    .append(r.getInt(8)==1 ? r.getDouble(9)+" per wrong answer" : "Off").append("\n");
            quizzes.setText(b.toString());
        }catch(SQLException e){Msg.error(this,e);}
    }

    private void joinQuiz() {
        String code = accessCode.getText().trim().toUpperCase();

        if (code.isEmpty()) {
            Msg.show(this, "Enter the quiz access code.");
            return;
        }

        int quizId = -1;
        int minutes = 10;
        String cat = "";
        boolean neg = false;
        double penalty = 0;
        List<Question> qs = new ArrayList<>();

        String sql = """
            SELECT q.quiz_id, q.title, c.category_name,
            q.duration_minutes, q.negative_marking_enabled,
            q.negative_marks, q.starts_at, q.ends_at
            FROM quizzes q
            JOIN categories c ON c.category_id = q.category_id
            WHERE q.access_code = ? AND q.status = 'PUBLISHED'
            """;

        try (Connection c = DB.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, code);

            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    Msg.show(this, "No published quiz found for that code.");
                    return;
                }

                quizId = r.getInt(1);
                cat = r.getString(3);
                minutes = r.getInt(4);
                neg = r.getInt(5) == 1;
                penalty = r.getDouble(6);

                String start = r.getString(7);
                String end = r.getString(8);

                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter fmt =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                if (start != null && !start.isBlank()
                        && now.isBefore(LocalDateTime.parse(start, fmt))) {
                    Msg.show(this, "This quiz is scheduled to start at " + start);
                    return;
                }

                if (end != null && !end.isBlank()
                        && now.isAfter(LocalDateTime.parse(end, fmt))) {
                    Msg.show(this, "The scheduled time for this quiz has ended.");
                    return;
                }
            }

            try (PreparedStatement q = c.prepareStatement("""
                SELECT x.question_id, x.question_text,
                x.option_a, x.option_b, x.option_c, x.option_d,
                x.correct_option, x.difficulty, x.topic, x.explanation
                FROM quiz_questions z
                JOIN questions x ON x.question_id = z.question_id
                WHERE z.quiz_id = ?
                ORDER BY z.question_id
                """)) {

                q.setInt(1, quizId);

                try (ResultSet a = q.executeQuery()) {
                    while (a.next()) {
                        qs.add(new Question(
                            a.getInt(1), a.getString(2),
                            a.getString(3), a.getString(4),
                            a.getString(5), a.getString(6),
                            a.getString(7), a.getString(8),
                            a.getString(9), a.getString(10)
                        ));
                    }
                }
            }

        } catch (SQLException e) {
            Msg.error(this, e);
            return;
        }

        if (qs.isEmpty()) {
            Msg.show(this, "This quiz has no questions.");
            return;
        }

        // Open the quiz only after all database resources have been closed.
        new QuizFrame(userId, name, role, cat,
                      qs, minutes, quizId, neg, penalty);
        dispose();
    }
}