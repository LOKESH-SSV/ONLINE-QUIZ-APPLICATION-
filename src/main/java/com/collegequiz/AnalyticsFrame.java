package com.collegequiz;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class AnalyticsFrame extends Frame {
    public AnalyticsFrame(){
        super("Teacher - Quiz Analytics");setSize(720,440);setLocationRelativeTo(null);setLayout(new BorderLayout());
        TextArea area=new TextArea();area.setEditable(false);add(area,BorderLayout.CENTER);
        StringBuilder b=new StringBuilder("SUBJECT-WISE PERFORMANCE\n\nSUBJECT | ATTEMPTS | UNIQUE STUDENTS | AVERAGE SCORE | BEST SCORE\n");
        String sql="SELECT c.category_name,COUNT(r.result_id),COUNT(DISTINCT r.user_id),AVG(r.score_percent),MAX(r.score_percent) FROM results r JOIN categories c ON c.category_id=r.category_id GROUP BY c.category_id ORDER BY c.category_name";
        try(Connection c=DB.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){
            while(r.next())b.append(r.getString(1)).append(" | ").append(r.getInt(2)).append(" | ").append(r.getInt(3)).append(" | ").append(String.format("%.1f%%",r.getDouble(4))).append(" | ").append(String.format("%.1f%%",r.getDouble(5))).append("\n");
            b.append("\nRECENT STUDENT RESULTS\n");
            try(ResultSet x=s.executeQuery("SELECT u.full_name,c.category_name,r.score_percent,r.attempted_at FROM results r JOIN users u ON u.user_id=r.user_id JOIN categories c ON c.category_id=r.category_id ORDER BY r.result_id DESC LIMIT 20")){
                while(x.next())b.append(x.getString(1)).append(" | ").append(x.getString(2)).append(" | ").append(String.format("%.1f%%",x.getDouble(3))).append(" | ").append(x.getString(4)).append("\n");
            }
        }catch(SQLException e){Msg.error(this,e);}area.setText(b.toString());
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){dispose();}});setVisible(true);
    }
}