package com.collegequiz.redesign;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

final class MessageDialog extends Dialog {
    MessageDialog(Frame owner, String message) {
        super(owner, "QuizPilot", true);
        setLayout(new BorderLayout(12, 12));
        add(new Label(message, Label.CENTER), BorderLayout.CENTER);
        Button ok = new Button("OK");
        Panel actions = new Panel(); actions.add(ok);
        add(actions, BorderLayout.SOUTH);
        ok.addActionListener(event -> dispose());
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { dispose(); }
        });
        setSize(390, 150);
        setLocationRelativeTo(owner);
        setVisible(true);
    }
}