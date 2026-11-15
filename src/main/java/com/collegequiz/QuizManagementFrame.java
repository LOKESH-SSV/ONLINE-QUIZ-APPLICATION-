package com.collegequiz;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.UUID;

public class QuizManagementFrame extends Frame {
    private final int teacherId;
    private final TextField title=new TextField(24), duration=new TextField("10",6), count=new TextField("10",6), code=new TextField(12);
    private final Choice category=new Choice();
    private final TextArea list=new TextArea(12,55);
    public QuizManagementFrame(int teacherId) {
        super("Teacher - Quiz Management"); this.teacherId=teacherId;
        setSize(690,570); setLocationRelativeTo(null); setLayout(new BorderLayout(8,8));
        Panel form=new Panel(new GridLayout(0,2,7,7));
        form.add(new Label("Quiz title"));form.add(title);
        form.add(new Label("Subject"));form.add(category);
        form.add(new Label("Duration (minutes)"));form.add(duration);
        form.add(new Label("Number of questions"));form.add(count);
        form.add(new Label("Access code (optional)"));form.add(code);
        Button create=new Button("Create and Publish Quiz");
        Button refresh=new Button("Refresh"); Button closeQuiz=new Button("Close Selected Quiz");
        Panel actions=new Panel();actions.add(create);actions.add(refresh);actions.add(closeQuiz);
        Panel north=new Panel(new BorderLayout());north.add(form,BorderLayout.CENTER);north.add(actions,BorderLayout.SOUTH);
        add(north,BorderLayout.NORTH);list.setEditable(false);add(list,BorderLayout.CENTER);
        create.addActionListener(e->createQuiz());refresh.addActionListener(e->loadQuizzes());
        closeQuiz.addActionListener(e->closeSelected());
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){dispose();}});
        loadCategories();loadQuizzes();setVisible(true);
    }
    private void loadCategories(){
        try(Connection c=DB.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT category_name FROM categories ORDER BY category_name")){
            while(r.next())category.add(r.getString(1));
        }catch(SQLException e){Msg.error(this,e);}
    }
    private void createQuiz(){
        String t=title.getText().trim(); int mins,n;
        try{mins=Integer.parseInt(duration.getText().trim());n=Integer.parseInt(count.getText().trim());}
        catch(NumberFormatException e){Msg.show(this,"Duration and question count must be numbers.");return;}
        if(t.isEmpty()||mins<1||n<1){Msg.show(this,"Enter a title, positive duration and positive question count.");return;}
        String access=code.getText().trim();if(access.isEmpty())access=UUID.randomUUID().toString().substring(0,6).toUpperCase();
        String sql="INSERT INTO quizzes(teacher_id,title,category_id,duration_minutes,question_count,access_code,status) SELECT ?,?,category_id,?,?,?,'PUBLISHED' FROM categories WHERE category_name=?";
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            p.setInt(1,teacherId);p.setString(2,t);p.setInt(3,mins);p.setInt(4,n);p.setString(5,access);p.setString(6,category.getSelectedItem());
            p.executeUpdate();int id=-1;try(ResultSet k=p.getGeneratedKeys()){if(k.next())id=k.getInt(1);}
            if(id<0)try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT last_insert_rowid()")){if(r.next())id=r.getInt(1);}
            try(PreparedStatement q=c.prepareStatement("INSERT INTO quiz_questions(quiz_id,question_id) SELECT ?,question_id FROM questions WHERE category_id=(SELECT category_id FROM categories WHERE category_name=?) ORDER BY RANDOM() LIMIT ?")){
                q.setInt(1,id);q.setString(2,category.getSelectedItem());q.setInt(3,n);int added=q.executeUpdate();
                if(added==0){try(PreparedStatement d=c.prepareStatement("DELETE FROM quizzes WHERE quiz_id=?")){d.setInt(1,id);d.executeUpdate();}Msg.show(this,"No questions exist for this subject. Add questions first.");return;}
                try(PreparedStatement u=c.prepareStatement("UPDATE quizzes SET question_count=? WHERE quiz_id=?")){u.setInt(1,added);u.setInt(2,id);u.executeUpdate();}
            }
            Msg.show(this,"Quiz published. Share this access code with students: "+access);
            title.setText("");code.setText("");loadQuizzes();
        }catch(SQLException e){Msg.error(this,e);}
    }
    private void loadQuizzes(){
        StringBuilder b=new StringBuilder("YOUR QUIZZES\n\nID | TITLE | SUBJECT | QUESTIONS | MINUTES | CODE | STATUS\n");
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement("SELECT q.quiz_id,q.title,c.category_name,q.question_count,q.duration_minutes,q.access_code,q.status FROM quizzes q JOIN categories c ON c.category_id=q.category_id WHERE q.teacher_id=? ORDER BY q.quiz_id DESC")){
            p.setInt(1,teacherId);try(ResultSet r=p.executeQuery()){while(r.next())b.append(r.getInt(1)).append(" | ").append(r.getString(2)).append(" | ").append(r.getString(3)).append(" | ").append(r.getInt(4)).append(" | ").append(r.getInt(5)).append(" | ").append(r.getString(6)).append(" | ").append(r.getString(7)).append("\n");}
            list.setText(b.toString());
        }catch(SQLException e){Msg.error(this,e);}
    }
    private void closeSelected(){
        String input=Msg.prompt(this,"Enter the quiz ID to close:");
        if(input==null)return;try{int id=Integer.parseInt(input.trim());
            try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement("UPDATE quizzes SET status='CLOSED' WHERE quiz_id=? AND teacher_id=?")){
                p.setInt(1,id);p.setInt(2,teacherId);int n=p.executeUpdate();Msg.show(this,n==1?"Quiz closed.":"Quiz ID not found.");loadQuizzes();
            }
        }catch(Exception e){Msg.show(this,"Enter a valid quiz ID.");}
    }
}