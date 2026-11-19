package com.collegequiz;

import java.awt.*;
import java.awt.event.*;

public final class Msg {
    private Msg() {}
    public static void show(Frame owner,String message){new DialogBox(owner,message,"Message");}
    public static void error(Frame owner,Exception ex){new DialogBox(owner,ex.getMessage(),"Error");}
    public static boolean confirm(Frame owner, String message) {
        Dialog d = new Dialog(owner, "Confirm", true);
        d.setLayout(new BorderLayout(10, 10));
        d.add(new Label(message), BorderLayout.CENTER);
        final boolean[] answer = {false};
        Panel buttons = new Panel();
        Button yes = new Button("Yes, Submit"), no = new Button("Continue Quiz");
        buttons.add(yes); buttons.add(no); d.add(buttons, BorderLayout.SOUTH);
        yes.addActionListener(e -> { answer[0] = true; d.dispose(); });
        no.addActionListener(e -> d.dispose());
        d.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){d.dispose();}});
        d.setSize(420,140); d.setLocationRelativeTo(owner); d.setVisible(true);
        return answer[0];
    }
    public static String prompt(Frame owner,String message){
        Dialog d=new Dialog(owner,"Input",true);d.setLayout(new BorderLayout(8,8));
        d.add(new Label(message),BorderLayout.NORTH);TextField input=new TextField(18);d.add(input,BorderLayout.CENTER);
        final String[] value={null};Panel p=new Panel();Button ok=new Button("OK"),cancel=new Button("Cancel");p.add(ok);p.add(cancel);d.add(p,BorderLayout.SOUTH);
        ok.addActionListener(e->{value[0]=input.getText();d.dispose();});cancel.addActionListener(e->d.dispose());
        d.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){d.dispose();}});
        d.setSize(330,130);d.setLocationRelativeTo(owner);d.setVisible(true);return value[0];
    }
    private static class DialogBox extends Dialog {
        DialogBox(Frame owner,String message,String title){
            super(owner,title,true);setLayout(new BorderLayout(12,12));
            add(new Label(message==null?"An unexpected error occurred.":message),BorderLayout.CENTER);
            Button ok=new Button("OK");Panel p=new Panel();p.add(ok);add(p,BorderLayout.SOUTH);
            ok.addActionListener(e->dispose());addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){dispose();}});
            setSize(440,160);setLocationRelativeTo(owner);setVisible(true);
        }
    }
}