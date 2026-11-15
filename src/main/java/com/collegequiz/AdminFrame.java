package com.collegequiz;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class AdminFrame extends Frame {
    private final Choice category=new Choice();private final TextArea question=new TextArea(3,35);
    private final TextField[] opts={new TextField(28),new TextField(28),new TextField(28),new TextField(28)};
    private final Choice correct=new Choice();
    public AdminFrame(){
        super("Teacher Panel - Add Question");setSize(600,500);setLocationRelativeTo(null);setLayout(new GridBagLayout());
        GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(6,8,6,8);c.anchor=GridBagConstraints.WEST;
        addRow(c,0,"Category",category);addRow(c,1,"Question",question);
        for(int i=0;i<4;i++)addRow(c,i+2,"Option "+(char)('A'+i),opts[i]);
        for(String x:new String[]{"A","B","C","D"})correct.add(x);
        addRow(c,6,"Correct option",correct);Button save=new Button("Save Question");
        c.gridx=1;c.gridy=7;add(save,c);save.addActionListener(e->saveQuestion());
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){dispose();}});
        loadCategories();setVisible(true);
    }
    private void addRow(GridBagConstraints c,int y,String label,Component field){c.gridx=0;c.gridy=y;add(new Label(label),c);c.gridx=1;add(field,c);}
    private void loadCategories(){
        try(Connection con=DB.getConnection();Statement st=con.createStatement();ResultSet rs=st.executeQuery("SELECT category_name FROM categories")){
            while(rs.next())category.add(rs.getString(1));
        }catch(SQLException ex){Msg.error(this,ex);}
    }
    private void saveQuestion(){
        if(question.getText().trim().isEmpty()){Msg.show(this,"Enter a question.");return;}
        for(TextField f:opts)if(f.getText().trim().isEmpty()){Msg.show(this,"Fill all four options.");return;}
        String sql="INSERT INTO questions(category_id,question_text,option_a,option_b,option_c,option_d,correct_option) SELECT category_id,?,?,?,?,?,? FROM categories WHERE category_name=?";
        try(Connection con=DB.getConnection();PreparedStatement ps=con.prepareStatement(sql)){
            ps.setString(1,question.getText().trim());for(int i=0;i<4;i++)ps.setString(i+2,opts[i].getText().trim());
            ps.setString(6,correct.getSelectedItem());ps.setString(7,category.getSelectedItem());ps.executeUpdate();
            Msg.show(this,"Question saved.");question.setText("");for(TextField f:opts)f.setText("");
        }catch(SQLException ex){Msg.error(this,ex);}
    }
}
