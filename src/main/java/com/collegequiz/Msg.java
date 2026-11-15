package com.collegequiz;

import java.awt.*;
import java.awt.event.*;

public final class Msg {
    private Msg() {}
    public static void show(Frame owner, String message) { new DialogBox(owner, message, "Message"); }
    public static void error(Frame owner, Exception ex) { new DialogBox(owner, ex.getMessage(), "Error"); }

    private static class DialogBox extends Dialog {
        DialogBox(Frame owner, String message, String title) {
            super(owner, title, true);
            setLayout(new BorderLayout(12, 12));
            add(new Label(message == null ? "An unexpected error occurred." : message), BorderLayout.CENTER);
            Button ok = new Button("OK");
            Panel panel = new Panel(); panel.add(ok); add(panel, BorderLayout.SOUTH);
            ok.addActionListener(e -> dispose());
            addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { dispose(); } });
            setSize(440, 160); setLocationRelativeTo(owner); setVisible(true);
        }
    }
}
