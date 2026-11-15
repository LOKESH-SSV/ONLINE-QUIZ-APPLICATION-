package com.collegequiz;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class QuizFrame extends Frame {
    private final int userId; private final String userName,role,category; private final List<Question> questions;
    private final Map<Integer,String> answers=new HashMap<>();
    private int index=0,secondsLeft=600; private final Label timer=new Label("Time: 10:00");
    private final Label questionLabel=new Label("",Label.LEFT);
    private final CheckboxGroup group=new CheckboxGroup(); private final Checkbox[] options=new Checkbox[4];
    private final Button next=new Button("Next"),submit=new Button("Submit Quiz");
    private volatile boolean running=true;
    public QuizFrame(int id,String name,String userRole,String cat,List<Question> qs){
        super("Quiz - "+cat);userId=id;userName=name;role=userRole;category=cat;questions=qs;
        setSize(720,440);setLocationRelativeTo(null);setLayout(new BorderLayout(15,15));
        Panel top=new Panel(new BorderLayout());top.add(new Label("Category: "+cat),BorderLayout.WEST);
        timer.setFont(new Font("Arial",Font.BOLD,17));timer.setForeground(new Color(180,45,45));top.add(timer,BorderLayout.EAST);add(top,BorderLayout.NORTH);
        Panel center=new Panel(new GridLayout(5,1,5,8));questionLabel.setFont(new Font("Arial",Font.BOLD,17));center.add(questionLabel);
        for(int i=0;i<4;i++){options[i]=new Checkbox("",group,false);options[i].setFont(new Font("Arial",Font.PLAIN,15));center.add(options[i]);}
        add(center,BorderLayout.CENTER);Panel bottom=new Panel(new FlowLayout(FlowLayout.RIGHT));bottom.add(next);bottom.add(submit);add(bottom,BorderLayout.SOUTH);
        next.addActionListener(e->saveAndNext());submit.addActionListener(e->finish());
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){if(running)finish();}});
        showQuestion();setVisible(true);
        Thread t=new Thread(()->{while(running&&secondsLeft>0){try{Thread.sleep(1000);}catch(InterruptedException e){return;}
            secondsLeft--;EventQueue.invokeLater(()->timer.setText(String.format("Time: %02d:%02d",secondsLeft/60,secondsLeft%60)));
        }if(running)EventQueue.invokeLater(this::finish);},"quiz-countdown");
        t.setDaemon(true);t.start();
    }
    private void showQuestion(){
        Question q=questions.get(index);questionLabel.setText("Question "+(index+1)+" of "+questions.size()+": "+q.text);
        String[] vals={q.a,q.b,q.c,q.d};String saved=answers.get(q.id);group.setSelectedCheckbox(null);
        for(int i=0;i<4;i++){options[i].setLabel((char)('A'+i)+". "+vals[i]);options[i].setState(saved!=null&&saved.equals(String.valueOf((char)('A'+i))));}
        next.setEnabled(index<questions.size()-1);
    }
    private void saveCurrent(){for(int i=0;i<4;i++)if(options[i].getState())answers.put(questions.get(index).id,String.valueOf((char)('A'+i)));}
    private void saveAndNext(){saveCurrent();if(index<questions.size()-1){index++;showQuestion();}}
    private void finish(){
        if(!running)return;running=false;saveCurrent();int score=0;
        for(Question q:questions)if(q.correct.equals(answers.get(q.id)))score++;
        double percent=100.0*score/questions.size();int elapsed=600-secondsLeft;
        String sql="INSERT INTO results(user_id,category_id,total_questions,correct_answers,score_percent,time_taken_seconds) SELECT ?,category_id,?,?,?,? FROM categories WHERE category_name=?";
        try(Connection con=DB.getConnection();PreparedStatement ps=con.prepareStatement(sql)){
            ps.setInt(1,userId);ps.setInt(2,questions.size());ps.setInt(3,score);ps.setDouble(4,percent);ps.setInt(5,elapsed);ps.setString(6,category);ps.executeUpdate();
            Msg.show(this,String.format("QUIZ COMPLETED!\n\nCorrect answers: %d / %d\nScore: %.2f%%\nTime taken: %d seconds",score,questions.size(),percent,elapsed));
            dispose();new DashboardFrame(userId,userName,role);
        }catch(SQLException ex){Msg.error(this,ex);dispose();}
    }
}
