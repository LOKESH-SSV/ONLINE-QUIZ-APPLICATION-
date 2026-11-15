package com.collegequiz;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class HistoryFrame extends Frame {
    private final int userId;
    private final TextArea area = new TextArea();
    public HistoryFrame(int userId) {
        super("My Quiz Results and Performance"); this.userId=userId;
        setSize(850,520);setLocationRelativeTo(null);setLayout(new BorderLayout(8,8));
        area.setEditable(false);add(area,BorderLayout.CENTER);
        Panel actions=new Panel();
        Button refresh=new Button("Refresh"),csv=new Button("Export CSV"),pdf=new Button("Export PDF");
        actions.add(refresh);actions.add(csv);actions.add(pdf);add(actions,BorderLayout.SOUTH);
        refresh.addActionListener(e->load());
        csv.addActionListener(e->ReportExporter.exportCsv(this,
                "SELECT q.title AS quiz,u.full_name AS student,a.total_questions,a.correct_answers,a.incorrect_answers,a.unanswered_answers,a.raw_score,a.negative_marks_applied,a.score_percent,a.time_taken_seconds,a.submitted_at FROM attempts a JOIN quizzes q ON q.quiz_id=a.quiz_id JOIN users u ON u.user_id=a.user_id WHERE a.user_id="+userId+" AND a.status='SUBMITTED' ORDER BY a.submitted_at DESC",
                "my_quiz_results"));
        pdf.addActionListener(e->ReportExporter.exportPdf(this,"My Quiz Performance",area.getText(),"my_quiz_performance"));
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){dispose();}});
        load();setVisible(true);
    }
    private void load() {
        StringBuilder b=new StringBuilder("MY QUIZ ATTEMPTS\n\nQUIZ | CORRECT | WRONG | UNANSWERED | RAW SCORE | PENALTY | PERCENT | TIME\n");
        String sql="""
            SELECT q.title,a.correct_answers,a.incorrect_answers,a.unanswered_answers,a.raw_score,
            a.negative_marks_applied,a.score_percent,a.time_taken_seconds,a.submitted_at
            FROM attempts a JOIN quizzes q ON q.quiz_id=a.quiz_id
            WHERE a.user_id=? AND a.status='SUBMITTED' ORDER BY a.submitted_at DESC""";
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,userId);
            try(ResultSet r=p.executeQuery()){
                while(r.next())b.append(r.getString(1)).append(" | ").append(r.getInt(2)).append(" | ")
                        .append(r.getInt(3)).append(" | ").append(r.getInt(4)).append(" | ")
                        .append(String.format("%.2f",r.getDouble(5))).append(" | ")
                        .append(String.format("%.2f",r.getDouble(6))).append(" | ")
                        .append(String.format("%.2f%%",r.getDouble(7))).append(" | ")
                        .append(r.getInt(8)).append(" sec | ").append(r.getString(9)).append("\n");
            }
            b.append("\nPRACTICE QUIZ HISTORY\n\nDATE | SUBJECT | CORRECT | TOTAL | SCORE | TIME\n");
            try(PreparedStatement practice=c.prepareStatement("""
                SELECT r.attempted_at,c.category_name,r.correct_answers,r.total_questions,
                r.score_percent,r.time_taken_seconds FROM results r JOIN categories c
                ON c.category_id=r.category_id WHERE r.user_id=? ORDER BY r.result_id DESC""")){
                practice.setInt(1,userId);try(ResultSet r=practice.executeQuery()){
                    while(r.next())b.append(r.getString(1)).append(" | ").append(r.getString(2)).append(" | ")
                            .append(r.getInt(3)).append(" | ").append(r.getInt(4)).append(" | ")
                            .append(String.format("%.1f%%",r.getDouble(5))).append(" | ")
                            .append(r.getInt(6)).append(" sec\n");
                }
            }
            b.append("\nTOPIC-WISE PERFORMANCE (submitted quizzes)\n\nTOPIC | ANSWERED | CORRECT | ACCURACY\n");
            try(PreparedStatement topic=c.prepareStatement("""
                SELECT COALESCE(NULLIF(q.topic,''),'General'),COUNT(*),
                SUM(sa.is_correct),ROUND(100.0*SUM(sa.is_correct)/COUNT(*),1)
                FROM student_answers sa JOIN attempts a ON a.attempt_id=sa.attempt_id
                JOIN questions q ON q.question_id=sa.question_id
                WHERE a.user_id=? AND a.status='SUBMITTED' AND sa.selected_option IS NOT NULL
                GROUP BY COALESCE(NULLIF(q.topic,''),'General') ORDER BY 1""")){
                topic.setInt(1,userId);try(ResultSet r=topic.executeQuery()){
                    while(r.next())b.append(r.getString(1)).append(" | ").append(r.getInt(2)).append(" | ")
                            .append(r.getInt(3)).append(" | ").append(r.getDouble(4)).append("%\n");
                }
            }
            area.setText(b.toString());
        }catch(SQLException e){Msg.error(this,e);}
    }
}