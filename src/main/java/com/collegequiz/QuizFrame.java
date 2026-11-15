package com.collegequiz;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class QuizFrame extends Frame {
    private final int userId,quizId; private final String userName,role,category; private final List<Question> questions;
    private final Map<Integer,String> answers=new HashMap<>();
    private int index=0,secondsLeft; private final int totalSeconds;
    private final Label timer=new Label(); private final Label questionLabel=new Label("",Label.LEFT);
    private final CheckboxGroup group=new CheckboxGroup(); private final Checkbox[] options=new Checkbox[4];
    private final Button previous=new Button("Previous"),next=new Button("Next"),submit=new Button("Submit Quiz");
    private volatile boolean running=true;

    public QuizFrame(int id,String name,String role,String cat,List<Question> qs){this(id,name,role,cat,qs,10,-1);}
    public QuizFrame(int id,String name,String role,String cat,List<Question> qs,int minutes,int quizId){
        super("Quiz - "+cat);userId=id;userName=name;this.role=role;category=cat;questions=qs;this.quizId=quizId;
        totalSeconds=Math.max(60,minutes*60);secondsLeft=totalSeconds;
        setSize(760,490);setLocationRelativeTo(null);setLayout(new BorderLayout(15,15));
        Panel top=new Panel(new BorderLayout());top.add(new Label("Category: "+cat+"     Questions: "+qs.size()),BorderLayout.WEST);
        timer.setFont(new Font("Arial",Font.BOLD,17));timer.setForeground(new Color(180,45,45));updateTimer();top.add(timer,BorderLayout.EAST);add(top,BorderLayout.NORTH);
        Panel center=new Panel(new GridLayout(5,1,5,8));questionLabel.setFont(new Font("Arial",Font.BOLD,17));center.add(questionLabel);
        for(int i=0;i<4;i++){options[i]=new Checkbox("",group,false);options[i].setFont(new Font("Arial",Font.PLAIN,15));center.add(options[i]);}
        add(center,BorderLayout.CENTER);Panel bottom=new Panel(new FlowLayout(FlowLayout.RIGHT));bottom.add(previous);bottom.add(next);bottom.add(submit);add(bottom,BorderLayout.SOUTH);
        previous.addActionListener(e->{saveCurrent();if(index>0){index--;showQuestion();}});
        next.addActionListener(e->{saveCurrent();if(index<questions.size()-1){index++;showQuestion();}});
        submit.addActionListener(e->confirmFinish());
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){confirmFinish();}});
        showQuestion();setVisible(true);
        Thread t=new Thread(()->{while(running&&secondsLeft>0){try{Thread.sleep(1000);}catch(InterruptedException e){return;}
            secondsLeft--;EventQueue.invokeLater(this::updateTimer);
        }if(running)EventQueue.invokeLater(()->finish(true));},"quiz-countdown");t.setDaemon(true);t.start();
    }
    private void updateTimer(){timer.setText(String.format("Time remaining: %02d:%02d",secondsLeft/60,secondsLeft%60));}
    private void showQuestion(){
        Question q=questions.get(index);questionLabel.setText("Question "+(index+1)+" of "+questions.size()+": "+q.text);
        String[] vals={q.a,q.b,q.c,q.d};String saved=answers.get(q.id);group.setSelectedCheckbox(null);
        for(int i=0;i<4;i++){options[i].setLabel((char)('A'+i)+". "+vals[i]);options[i].setState(saved!=null&&saved.equals(String.valueOf((char)('A'+i))));}
        previous.setEnabled(index>0);next.setEnabled(index<questions.size()-1);
    }
    private void saveCurrent(){for(int i=0;i<4;i++)if(options[i].getState())answers.put(questions.get(index).id,String.valueOf((char)('A'+i)));}
    private void confirmFinish(){if(!running)return;saveCurrent();Msg.show(this,"Your answers will be submitted now.");finish(false);}
    private void finish(boolean timedOut){
        if(!running)return;running=false;saveCurrent();int score=0;
        for(Question q:questions)if(q.correct.equals(answers.get(q.id)))score++;
        double percent=100.0*score/questions.size();int elapsed=totalSeconds-secondsLeft;
        String sql="INSERT INTO results(user_id,category_id,total_questions,correct_answers,score_percent,time_taken_seconds) SELECT ?,category_id,?,?,?,? FROM categories WHERE category_name=?";
        try(Connection con=DB.getConnection()){
            con.setAutoCommit(false);
            try(PreparedStatement ps=con.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
                ps.setInt(1,userId);ps.setInt(2,questions.size());ps.setInt(3,score);ps.setDouble(4,percent);ps.setInt(5,elapsed);ps.setString(6,category);ps.executeUpdate();
            }
            if(quizId>0){
                int attemptId;
                try(PreparedStatement p=con.prepareStatement("INSERT INTO attempts(quiz_id,user_id,total_questions,correct_answers,score_percent,time_taken_seconds) VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                    p.setInt(1,quizId);p.setInt(2,userId);p.setInt(3,questions.size());p.setInt(4,score);p.setDouble(5,percent);p.setInt(6,elapsed);p.executeUpdate();
                    try(ResultSet k=p.getGeneratedKeys()){if(!k.next())throw new SQLException("Could not create attempt record.");attemptId=k.getInt(1);}
                }
                try(PreparedStatement p=con.prepareStatement("INSERT INTO student_answers(attempt_id,question_id,selected_option,is_correct) VALUES(?,?,?,?)")){
                    for(Question q:questions){String selected=answers.get(q.id);p.setInt(1,attemptId);p.setInt(2,q.id);p.setString(3,selected);p.setInt(4,q.correct.equals(selected)?1:0);p.addBatch();}p.executeBatch();
                }
            }
            con.commit();
            Msg.show(this,(timedOut?"Time is over. Your quiz was auto-submitted.\n\n":"QUIZ COMPLETED!\n\n")+String.format("Correct answers: %d / %d\nScore: %.2f%%\nTime taken: %d seconds",score,questions.size(),percent,elapsed));
            dispose();new DashboardFrame(userId,userName,role);
        }catch(SQLException ex){Msg.error(this,ex);dispose();}
    }
}