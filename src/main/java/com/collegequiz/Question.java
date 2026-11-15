package com.collegequiz;

public class Question {
    public final int id;
    public final String text, a, b, c, d, correct, difficulty, topic, explanation;

    public Question(int id, String text, String a, String b, String c, String d, String correct) {
        this(id, text, a, b, c, d, correct, "MEDIUM", "", "");
    }

    public Question(int id, String text, String a, String b, String c, String d,
                    String correct, String difficulty, String topic, String explanation) {
        this.id = id;
        this.text = text;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.correct = correct;
        this.difficulty = difficulty == null ? "MEDIUM" : difficulty;
        this.topic = topic == null ? "" : topic;
        this.explanation = explanation == null ? "" : explanation;
    }
}
