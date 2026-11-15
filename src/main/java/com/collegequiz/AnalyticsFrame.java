package com.collegequiz;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class AnalyticsFrame extends Frame {
    private final int teacherId;
    private final TextArea area = new TextArea();
    public AnalyticsFrame(int teacherId) {
        super("Teacher - Reports and Analytics"); this.teacherId=teacherId;
        setSize(900,560);setLocationRelativeTo(null);setLayout(new BorderLayout(8,8));
        area.setEditable(false);add(area,BorderLayout.CENTER);
        Panel actions=new Panel();
        Button refresh=new Button("Refresh"),csv=new Button("Export CSV"),pdf=new Button("Export PDF");
        actions.add(refresh);actions.add(csv);actions.add(pdf);add(actions,BorderLayout.SOUTH);
        refresh.addActionListener(e->load());
        csv.addActionListener(e->ReportExporter.exportCsv(this,reportSql(),"teacher_quiz_report"));
        pdf.addActionListener(e->ReportExporter.exportPdf(this,"Teacher Quiz Analytics",area.getText(),"teacher_quiz_analytics"));
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){dispose();}});
        load();setVisible(true);
    }
    private String reportSql(){
        return "SELECT q.title AS quiz,u.full_name AS student,a.total_questions,a.correct_answers,a.incorrect_answers,a.unanswered_answers,a.raw_score,a.negative_marks_applied,a.score_percent,a.time_taken_seconds,a.submitted_at FROM attempts a JOIN quizzes q ON q.quiz_id=a.quiz_id JOIN users u ON u.user_id=a.user_id WHERE q.teacher_id="+teacherId+" AND a.status='SUBMITTED' ORDER BY a.submitted_at DESC";
    }
    private void load(){
        StringBuilder b=new StringBuilder("TEACHER QUIZ PERFORMANCE\n\nQUIZ | ATTEMPTS | STUDENTS | AVG SCORE | BEST SCORE\n");
        String summary="""
            SELECT q.quiz_id,q.title,COUNT(a.attempt_id),COUNT(DISTINCT a.user_id),
            COALESCE(AVG(a.score_percent),0),COALESCE(MAX(a.score_percent),0)
            FROM quizzes q LEFT JOIN attempts a ON a.quiz_id=q.quiz_id AND a.status='SUBMITTED'
            WHERE q.teacher_id=? GROUP BY q.quiz_id,q.title ORDER BY q.quiz_id DESC""";
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(summary)){
            p.setInt(1,teacherId);try(ResultSet r=p.executeQuery()){
                while(r.next())b.append(r.getString(2)).append(" | ").append(r.getInt(3)).append(" | ")
                        .append(r.getInt(4)).append(" | ").append(String.format("%.1f%%",r.getDouble(5)))
                        .append(" | ").append(String.format("%.1f%%",r.getDouble(6))).append("\n");
            }
            b.append("\nRECENT STUDENT ATTEMPTS\n\nQUIZ | STUDENT | CORRECT | WRONG | UNANSWERED | RAW SCORE | PENALTY | PERCENT | TIME\n");
            try(PreparedStatement recent=c.prepareStatement("""
                SELECT q.title,u.full_name,a.correct_answers,a.incorrect_answers,a.unanswered_answers,
                a.raw_score,a.negative_marks_applied,a.score_percent,a.time_taken_seconds
                FROM attempts a JOIN quizzes q ON q.quiz_id=a.quiz_id JOIN users u ON u.user_id=a.user_id
                WHERE q.teacher_id=? AND a.status='SUBMITTED' ORDER BY a.submitted_at DESC LIMIT 50""")){
                recent.setInt(1,teacherId);try(ResultSet r=recent.executeQuery()){
                    while(r.next())b.append(r.getString(1)).append(" | ").append(r.getString(2)).append(" | ")
                            .append(r.getInt(3)).append(" | ").append(r.getInt(4)).append(" | ")
                            .append(r.getInt(5)).append(" | ").append(String.format("%.2f",r.getDouble(6)))
                            .append(" | ").append(String.format("%.2f",r.getDouble(7))).append(" | ")
                            .append(String.format("%.1f%%",r.getDouble(8))).append(" | ")
                            .append(r.getInt(9)).append(" sec\n");
                }
            }
            b.append("\nQUESTION BANK SUMMARY\n\nSUBJECT | DIFFICULTY | QUESTIONS\n");
            try(PreparedStatement bank=c.prepareStatement("""
                SELECT c.category_name,q.difficulty,COUNT(*) FROM questions q
                JOIN categories c ON c.category_id=q.category_id
                WHERE q.question_id IN (SELECT qq.question_id FROM quiz_questions qq
                  JOIN quizzes z ON z.quiz_id=qq.quiz_id WHERE z.teacher_id=?)
                GROUP BY c.category_name,q.difficulty ORDER BY c.category_name,q.difficulty""")){
                bank.setInt(1,teacherId);try(ResultSet r=bank.executeQuery()){
                    while(r.next())b.append(r.getString(1)).append(" | ").append(r.getString(2))
                            .append(" | ").append(r.getInt(3)).append("\n");
                }
            }
            area.setText(b.toString());
        }catch(SQLException e){Msg.error(this,e);}
    }
}