package com.collegequiz;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class AdminFrame extends Frame {
    private final Choice category=new Choice(), questionList=new Choice();
    private final TextArea question=new TextArea(3,35);
    private final TextField[] opts={new TextField(28),new TextField(28),new TextField(28),new TextField(28)};
    private final Choice correct=new Choice();
    private int selectedId=-1;
    public AdminFrame(){
        super("Teacher Panel - Question Management");setSize(650,590);setLocationRelativeTo(null);setLayout(new GridBagLayout());
        GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(6,8,6,8);c.anchor=GridBagConstraints.WEST;
        addRow(c,0,"Category",category);addRow(c,1,"Saved questions",questionList);addRow(c,2,"Question",question);
        for(int i=0;i<4;i++)addRow(c,i+3,"Option "+(char)('A'+i),opts[i]);
        for(String x:new String[]{"A","B","C","D"})correct.add(x);
        addRow(c,7,"Correct option",correct);
        Button save=new Button("Add Question"),update=new Button("Update Selected"),delete=new Button("Delete Selected"),clear=new Button("Clear");
        Panel buttons=new Panel();buttons.add(save);buttons.add(update);buttons.add(delete);buttons.add(clear);
        c.gridx=0;c.gridy=8;c.gridwidth=2;add(buttons,c);
        save.addActionListener(e->saveQuestion());update.addActionListener(e->updateQuestion());delete.addActionListener(e->deleteQuestion());clear.addActionListener(e->clearForm());
        category.addItemListener(e->loadQuestions());questionList.addItemListener(e->loadSelected());
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){dispose();}});
        loadCategories();setVisible(true);
    }
    private void addRow(GridBagConstraints c,int y,String label,Component field){c.gridx=0;c.gridy=y;c.gridwidth=1;add(new Label(label),c);c.gridx=1;add(field,c);}
    private void loadCategories(){
        try(Connection con=DB.getConnection();Statement st=con.createStatement();ResultSet rs=st.executeQuery("SELECT category_name FROM categories ORDER BY category_name")){
            while(rs.next())category.add(rs.getString(1));
        }catch(SQLException ex){Msg.error(this,ex);}loadQuestions();
    }
    private boolean valid(){
        if(question.getText().trim().isEmpty()){Msg.show(this,"Enter a question.");return false;}
        for(TextField f:opts)if(f.getText().trim().isEmpty()){Msg.show(this,"Fill all four options.");return false;}return true;
    }
    private void saveQuestion(){
        if(!valid())return;
        String sql="INSERT INTO questions(category_id,question_text,option_a,option_b,option_c,option_d,correct_option) SELECT category_id,?,?,?,?,?,? FROM categories WHERE category_name=?";
        try(Connection con=DB.getConnection();PreparedStatement ps=con.prepareStatement(sql)){
            bind(ps);ps.executeUpdate();Msg.show(this,"Question added.");clearForm();loadQuestions();
        }catch(SQLException ex){Msg.error(this,ex);}
    }
    private void bind(PreparedStatement ps)throws SQLException{
        ps.setString(1,question.getText().trim());for(int i=0;i<4;i++)ps.setString(i+2,opts[i].getText().trim());
        ps.setString(6,correct.getSelectedItem());ps.setString(7,category.getSelectedItem());
    }
    private void loadQuestions(){
        questionList.removeAll();selectedId=-1;
        try(Connection con=DB.getConnection();PreparedStatement ps=con.prepareStatement("SELECT question_id,question_text FROM questions WHERE category_id=(SELECT category_id FROM categories WHERE category_name=?) ORDER BY question_id")){
            ps.setString(1,category.getSelectedItem());try(ResultSet rs=ps.executeQuery()){while(rs.next())questionList.add(rs.getInt(1)+" - "+rs.getString(2));}
        }catch(SQLException ex){Msg.error(this,ex);}
    }
    private void loadSelected(){
        String item=questionList.getSelectedItem();if(item==null)return;
        try{selectedId=Integer.parseInt(item.substring(0,item.indexOf(" - ")));}
        catch(Exception ex){return;}
        try(Connection con=DB.getConnection();PreparedStatement ps=con.prepareStatement("SELECT question_text,option_a,option_b,option_c,option_d,correct_option FROM questions WHERE question_id=?")){
            ps.setInt(1,selectedId);try(ResultSet rs=ps.executeQuery()){if(rs.next()){
                question.setText(rs.getString(1));for(int i=0;i<4;i++)opts[i].setText(rs.getString(i+2));correct.select(rs.getString(6));
            }}
        }catch(SQLException ex){Msg.error(this,ex);}
    }
    private void updateQuestion(){
        if(selectedId<0){Msg.show(this,"Select a saved question first.");return;}if(!valid())return;
        String sql="UPDATE questions SET question_text=?,option_a=?,option_b=?,option_c=?,option_d=?,correct_option=? WHERE question_id=?";
        try(Connection con=DB.getConnection();PreparedStatement ps=con.prepareStatement(sql)){
            ps.setString(1,question.getText().trim());for(int i=0;i<4;i++)ps.setString(i+2,opts[i].getText().trim());ps.setString(6,correct.getSelectedItem());ps.setInt(7,selectedId);ps.executeUpdate();
            Msg.show(this,"Question updated.");loadQuestions();
        }catch(SQLException ex){Msg.error(this,ex);}
    }
    private void deleteQuestion(){
        if(selectedId<0){Msg.show(this,"Select a saved question first.");return;}
        try(Connection con=DB.getConnection();PreparedStatement ps=con.prepareStatement("DELETE FROM questions WHERE question_id=?")){
            ps.setInt(1,selectedId);ps.executeUpdate();Msg.show(this,"Question deleted.");clearForm();loadQuestions();
        }catch(SQLException ex){Msg.error(this,ex);}
    }
    private void clearForm(){selectedId=-1;question.setText("");for(TextField f:opts)f.setText("");}
}
