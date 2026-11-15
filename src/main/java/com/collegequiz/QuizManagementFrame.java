package com.collegequiz;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.UUID;

public class QuizManagementFrame extends Frame {
    private final int teacherId;
    private final TextField title=new TextField(24), duration=new TextField("10",6), count=new TextField("5",6), code=new TextField(12);
    private final Choice category=new Choice();
    private final TextArea list=new TextArea(12,55);

    public QuizManagementFrame(int teacherId) {
        super("Teacher - Quiz Management"); this.teacherId=teacherId;
        setSize(690,570); setLocationRelativeTo(null); setLayout(new BorderLayout(8,8));
        Panel form=new Panel(new GridLayout(0,2,7,7));
        form.add(new Label("Quiz title"));form.add(title);
        form.add(new Label("Subject"));form.add(category);
        form.add(new Label("Duration (minutes)"));form.add(duration);
        form.add(new Label("Number of questions to enter"));form.add(count);
        form.add(new Label("Access code (optional)"));form.add(code);
        Button create=new Button("Create Quiz and Enter Questions");
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
        String t=title.getText().trim();int mins,n;
        try{mins=Integer.parseInt(duration.getText().trim());n=Integer.parseInt(count.getText().trim());}
        catch(NumberFormatException e){Msg.show(this,"Duration and question count must be valid numbers.");return;}
        if(t.isEmpty()||mins<1||n<1||n>100){Msg.show(this,"Enter a title, duration above zero and 1 to 100 questions.");return;}
        String access=code.getText().trim();if(access.isEmpty())access=UUID.randomUUID().toString().substring(0,6).toUpperCase();
        String cat=category.getSelectedItem();
        try(Connection c=DB.getConnection()){
            c.setAutoCommit(false);
            try{
                int categoryId;
                try(PreparedStatement p=c.prepareStatement("SELECT category_id FROM categories WHERE category_name=?")){
                    p.setString(1,cat);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Choose a valid subject.");categoryId=r.getInt(1);}
                }
                int quizId;
                try(PreparedStatement p=c.prepareStatement("INSERT INTO quizzes(teacher_id,title,category_id,duration_minutes,question_count,access_code,status) VALUES(?,?,?,?,?,?,'DRAFT')",Statement.RETURN_GENERATED_KEYS)){
                    p.setInt(1,teacherId);p.setString(2,t);p.setInt(3,categoryId);p.setInt(4,mins);p.setInt(5,n);p.setString(6,access);p.executeUpdate();
                    try(ResultSet r=p.getGeneratedKeys()){if(!r.next())throw new SQLException("Could not create quiz.");quizId=r.getInt(1);}
                }
                for(int i=1;i<=n;i++){
                    String[] data=showQuestionDialog(i,n);
                    if(data==null){c.rollback();Msg.show(this,"Quiz creation cancelled. No incomplete quiz was saved.");return;}
                    int questionId;
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO questions(category_id,question_text,option_a,option_b,option_c,option_d,correct_option) VALUES(?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                        p.setInt(1,categoryId);for(int j=0;j<6;j++)p.setString(j+2,data[j]);p.executeUpdate();
                        try(ResultSet r=p.getGeneratedKeys()){if(!r.next())throw new SQLException("Could not save question "+i+".");questionId=r.getInt(1);}
                    }
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO quiz_questions(quiz_id,question_id) VALUES(?,?)")){
                        p.setInt(1,quizId);p.setInt(2,questionId);p.executeUpdate();
                    }
                }
                try(PreparedStatement p=c.prepareStatement("UPDATE quizzes SET status='PUBLISHED' WHERE quiz_id=?")){
                    p.setInt(1,quizId);p.executeUpdate();
                }
                c.commit();
                Msg.show(this,"Quiz published successfully!\nQuestions added: "+n+"\nShare this access code with students: "+access);
                title.setText("");code.setText("");loadQuizzes();
            }catch(Exception ex){c.rollback();throw ex;}
        }catch(SQLException e){Msg.error(this,e);}
    }
    // Returns question, A, B, C, D, correct option. Cancel returns null.
    private String[] showQuestionDialog(int number,int total){
        Dialog d=new Dialog(this,"Enter Question "+number+" of "+total,true);
        d.setLayout(new BorderLayout(8,8));d.setSize(520,390);d.setLocationRelativeTo(this);
        Panel fields=new Panel(new GridLayout(0,2,6,6));
        TextField q=new TextField(30);TextField[] o={new TextField(24),new TextField(24),new TextField(24),new TextField(24)};
        Choice answer=new Choice();for(String x:new String[]{"A","B","C","D"})answer.add(x);
        fields.add(new Label("Question"));fields.add(q);
        for(int i=0;i<4;i++){fields.add(new Label("Option "+(char)('A'+i)));fields.add(o[i]);}
        fields.add(new Label("Correct option"));fields.add(answer);
        d.add(new Label("Enter the question and all four options."),BorderLayout.NORTH);d.add(fields,BorderLayout.CENTER);
        final String[][] result={null};Panel buttons=new Panel();Button save=new Button(number==total?"Save Question and Publish":"Save Question and Continue");Button cancel=new Button("Cancel Quiz");
        buttons.add(save);buttons.add(cancel);d.add(buttons,BorderLayout.SOUTH);
        save.addActionListener(e->{
            if(q.getText().trim().isEmpty()){Msg.show(this,"Enter the question text.");return;}
            for(TextField f:o)if(f.getText().trim().isEmpty()){Msg.show(this,"Fill all four options.");return;}
            result[0]=new String[]{q.getText().trim(),o[0].getText().trim(),o[1].getText().trim(),o[2].getText().trim(),o[3].getText().trim(),answer.getSelectedItem()};
            d.dispose();
        });
        cancel.addActionListener(e->d.dispose());d.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){d.dispose();}});
        d.setVisible(true);return result[0];
    }
    private void loadQuizzes(){
        StringBuilder b=new StringBuilder("YOUR QUIZZES\n\nID | TITLE | SUBJECT | QUESTIONS | MINUTES | CODE | STATUS\n");
        try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement("SELECT q.quiz_id,q.title,c.category_name,q.question_count,q.duration_minutes,q.access_code,q.status FROM quizzes q JOIN categories c ON c.category_id=q.category_id WHERE q.teacher_id=? ORDER BY q.quiz_id DESC")){
            p.setInt(1,teacherId);try(ResultSet r=p.executeQuery()){while(r.next())b.append(r.getInt(1)).append(" | ").append(r.getString(2)).append(" | ").append(r.getString(3)).append(" | ").append(r.getInt(4)).append(" | ").append(r.getInt(5)).append(" | ").append(r.getString(6)).append(" | ").append(r.getString(7)).append("\n");}
            list.setText(b.toString());
        }catch(SQLException e){Msg.error(this,e);}
    }
    private void closeSelected(){
        String input=Msg.prompt(this,"Enter the quiz ID to close:");if(input==null)return;
        try{int id=Integer.parseInt(input.trim());
            try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement("UPDATE quizzes SET status='CLOSED' WHERE quiz_id=? AND teacher_id=?")){
                p.setInt(1,id);p.setInt(2,teacherId);int n=p.executeUpdate();Msg.show(this,n==1?"Quiz closed.":"Quiz ID not found.");loadQuizzes();
            }
        }catch(Exception e){Msg.show(this,"Enter a valid quiz ID.");}
    }
}