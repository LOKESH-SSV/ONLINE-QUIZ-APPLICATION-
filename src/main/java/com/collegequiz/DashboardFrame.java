package com.collegequiz;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class DashboardFrame extends Frame {
    private final int userId; private final String userName,role;
    private final Choice categories=new Choice();
    public DashboardFrame(int id,String name,String role){
        super("Online Quiz - Dashboard");userId=id;userName=name;this.role=role;
        setSize(650,440);setLocationRelativeTo(null);setLayout(new GridBagLayout());setBackground(new Color(245,248,255));
        GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(10,12,10,12);
        Label welcome=new Label("Welcome, "+name+"!");welcome.setFont(new Font("Arial",Font.BOLD,23));
        c.gridx=0;c.gridy=0;c.gridwidth=2;add(welcome,c);c.gridwidth=1;
        c.gridx=0;c.gridy=1;add(new Label("Practice category:"),c);c.gridx=1;add(categories,c);
        Button practice=new Button("Start Practice Quiz"),available=new Button("Available Quizzes"),history=new Button("My Results"),board=new Button("Leaderboard"),logout=new Button("Logout");
        c.gridx=0;c.gridy=2;add(practice,c);c.gridx=1;add(available,c);
        c.gridx=0;c.gridy=3;add(history,c);c.gridx=1;add(board,c);
        if("ADMIN".equals(role)){
            Button manage=new Button("Create / Manage Quizzes"),questions=new Button("Manage Questions"),analytics=new Button("View Analytics");
            c.gridx=0;c.gridy=4;add(manage,c);c.gridx=1;add(questions,c);
            c.gridx=0;c.gridy=5;c.gridwidth=2;add(analytics,c);
            manage.addActionListener(e->new QuizManagementFrame(userId));
            questions.addActionListener(e->new AdminFrame());
            analytics.addActionListener(e->new AnalyticsFrame());
        }
        c.gridx=0;c.gridy=6;c.gridwidth=2;add(logout,c);
        practice.addActionListener(e->startPractice());available.addActionListener(e->new AvailableQuizzesFrame(userId,userName,role));
        history.addActionListener(e->new HistoryFrame(userId));board.addActionListener(e->leaderboard());
        logout.addActionListener(e->{new AuthFrame();dispose();});
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){dispose();System.exit(0);}});
        loadCategories();setVisible(true);
    }
    private void loadCategories(){
        try(Connection con=DB.getConnection();Statement st=con.createStatement();ResultSet rs=st.executeQuery("SELECT category_name FROM categories ORDER BY category_name")){
            while(rs.next())categories.add(rs.getString(1));
        }catch(SQLException ex){Msg.error(this,ex);}
    }
    private void startPractice(){
        String cat=categories.getSelectedItem();
        try(Connection con=DB.getConnection();PreparedStatement ps=con.prepareStatement("""
          SELECT q.question_id,q.question_text,q.option_a,q.option_b,q.option_c,q.option_d,q.correct_option
          FROM questions q JOIN categories c ON q.category_id=c.category_id
          WHERE c.category_name=? ORDER BY RANDOM() LIMIT 10""")){
            ps.setString(1,cat);try(ResultSet rs=ps.executeQuery()){
                List<Question> qs=new ArrayList<>();while(rs.next())qs.add(new Question(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7)));
                if(qs.isEmpty()){Msg.show(this,"No questions in this category yet.");return;}
                new QuizFrame(userId,userName,role,cat,qs);dispose();
            }
        }catch(SQLException ex){Msg.error(this,ex);}
    }
    private void leaderboard(){
        String sql="SELECT u.full_name,c.category_name,MAX(r.score_percent) best FROM results r JOIN users u ON r.user_id=u.user_id JOIN categories c ON r.category_id=c.category_id GROUP BY u.user_id,c.category_id ORDER BY best DESC LIMIT 10";
        StringBuilder b=new StringBuilder("TOP 10 LEADERBOARD\n\n");
        try(Connection con=DB.getConnection();Statement st=con.createStatement();ResultSet rs=st.executeQuery(sql)){
            while(rs.next())b.append(rs.getString(1)).append(" | ").append(rs.getString(2)).append(" | ").append(String.format("%.2f",rs.getDouble(3))).append("%\n");
            Msg.show(this,b.toString());
        }catch(SQLException ex){Msg.error(this,ex);}
    }
}