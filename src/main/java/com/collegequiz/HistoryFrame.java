package com.collegequiz;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class HistoryFrame extends Frame {
    public HistoryFrame(int userId){
        super("My Quiz Results");setSize(690,400);setLocationRelativeTo(null);setLayout(new BorderLayout());
        TextArea area=new TextArea();area.setEditable(false);add(area,BorderLayout.CENTER);
        StringBuilder b=new StringBuilder("DATE | SUBJECT | CORRECT | TOTAL | SCORE | TIME\n\n");
        String sql="SELECT r.attempted_at,c.category_name,r.correct_answers,r.total_questions,r.score_percent,r.time_taken_seconds FROM results r JOIN categories c ON c.category_id=r.category_id WHERE r.user_id=? ORDER BY r.result_id DESC";
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,userId);try(ResultSet r=p.executeQuery()){while(r.next())b.append(r.getString(1)).append(" | ").append(r.getString(2)).append(" | ").append(r.getInt(3)).append(" | ").append(r.getInt(4)).append(" | ").append(String.format("%.1f%%",r.getDouble(5))).append(" | ").append(r.getInt(6)).append(" sec\n");}
        }catch(SQLException e){Msg.error(this,e);}area.setText(b.toString());
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){dispose();}});setVisible(true);
    }
}