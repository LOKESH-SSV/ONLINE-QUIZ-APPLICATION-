package com.collegequiz;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Main application UI and workflow. Uses Java AWT only.
 * Database tables are created/migrated by DB.initialize() plus initExtendedSchema().
 */
public final class App {
    private App() {}
    private static final String TEACHER_CODE = System.getProperty("quiz.teacherCode", "QUIZ-TEACHER-2026");
    private static final DateTimeFormatter SQL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void launch() {
        try {
            DB.initialize();
            initExtendedSchema();
            EventQueue.invokeLater(AuthWindow::new);
        } catch (Exception ex) {
            ex.printStackTrace();
            new ErrorDialog(null, "Database initialization failed: " + ex.getMessage());
        }
    }

    private static Connection db() throws SQLException { return DB.getConnection(); }
    private static void initExtendedSchema() throws SQLException {
        try (Connection c=db(); Statement s=c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS quizzes(
                  quiz_id INTEGER PRIMARY KEY AUTOINCREMENT,
                  title TEXT NOT NULL, subject TEXT NOT NULL, description TEXT DEFAULT '',
                  duration_minutes INTEGER NOT NULL DEFAULT 10,
                  marks_per_question REAL NOT NULL DEFAULT 1,
                  quiz_code TEXT NOT NULL UNIQUE,
                  teacher_id INTEGER NOT NULL,
                  status TEXT NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','PUBLISHED','CLOSED')),
                  start_at TEXT, end_at TEXT,
                  randomize INTEGER NOT NULL DEFAULT 1,
                  allow_review INTEGER NOT NULL DEFAULT 1,
                  created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                  FOREIGN KEY(teacher_id) REFERENCES users(user_id)
                )""");
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS quiz_questions(
                  quiz_id INTEGER NOT NULL, question_id INTEGER NOT NULL, position INTEGER NOT NULL DEFAULT 0,
                  PRIMARY KEY(quiz_id,question_id),
                  FOREIGN KEY(quiz_id) REFERENCES quizzes(quiz_id) ON DELETE CASCADE,
                  FOREIGN KEY(question_id) REFERENCES questions(question_id) ON DELETE CASCADE
                )""");
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS attempts(
                  attempt_id INTEGER PRIMARY KEY AUTOINCREMENT,
                  quiz_id INTEGER NOT NULL, user_id INTEGER NOT NULL,
                  started_at TEXT DEFAULT CURRENT_TIMESTAMP, submitted_at TEXT,
                  score REAL DEFAULT 0, total_marks REAL DEFAULT 0,
                  correct_count INTEGER DEFAULT 0, wrong_count INTEGER DEFAULT 0,
                  unanswered_count INTEGER DEFAULT 0, status TEXT DEFAULT 'IN_PROGRESS',
                  FOREIGN KEY(quiz_id) REFERENCES quizzes(quiz_id),
                  FOREIGN KEY(user_id) REFERENCES users(user_id)
                )""");
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS student_answers(
                  attempt_id INTEGER NOT NULL, question_id INTEGER NOT NULL,
                  selected_option TEXT, is_correct INTEGER DEFAULT 0, marked_review INTEGER DEFAULT 0,
                  PRIMARY KEY(attempt_id,question_id),
                  FOREIGN KEY(attempt_id) REFERENCES attempts(attempt_id) ON DELETE CASCADE,
                  FOREIGN KEY(question_id) REFERENCES questions(question_id)
                )""");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_quizzes_status ON quizzes(status)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_attempts_user ON attempts(user_id)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_answers_attempt ON student_answers(attempt_id)");
        }
        seedDemoQuizzes();
    }

    private static void seedDemoQuizzes() throws SQLException {
        try (Connection c=db()) {
            try (Statement st=c.createStatement(); ResultSet r=st.executeQuery("SELECT COUNT(*) FROM quizzes")) {
                if (r.next() && r.getInt(1)>0) return;
            }
            int teacherId;
            try (Statement st=c.createStatement(); ResultSet r=st.executeQuery("SELECT user_id FROM users WHERE role='ADMIN' ORDER BY user_id LIMIT 1")) {
                if (r.next()) teacherId=r.getInt(1);
                else {
                    try (PreparedStatement p=c.prepareStatement("INSERT INTO users(full_name,email,password_hash,role) VALUES(?,?,?,?)")) {
                        p.setString(1,"Demo Teacher"); p.setString(2,"demo.teacher@quiz.local");
                        p.setString(3,PasswordUtil.hash("Teacher@123")); p.setString(4,"ADMIN"); p.executeUpdate();
                    }
                    try (Statement q=c.createStatement(); ResultSet x=q.executeQuery("SELECT user_id FROM users WHERE email='demo.teacher@quiz.local'")) { x.next(); teacherId=x.getInt(1); }
                }
            }
            try (Statement st=c.createStatement(); ResultSet cats=st.executeQuery("SELECT category_id,category_name FROM categories")) {
                while(cats.next()) {
                    int catId=cats.getInt(1); String subject=cats.getString(2);
                    int count;
                    try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM questions WHERE category_id=?")){p.setInt(1,catId);try(ResultSet r=p.executeQuery()){r.next();count=r.getInt(1);}}
                    if(count==0) continue;
                    String code=("QUIZ"+(1000+catId));
                    int quizId;
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO quizzes(title,subject,description,duration_minutes,marks_per_question,quiz_code,teacher_id,status,randomize,allow_review) VALUES(?,?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                        p.setString(1,subject+" Practice Quiz");p.setString(2,subject);p.setString(3,"Sample quiz created for demonstration.");
                        p.setInt(4,10);p.setDouble(5,1);p.setString(6,code);p.setInt(7,teacherId);p.setString(8,"PUBLISHED");p.setInt(9,1);p.setInt(10,1);p.executeUpdate();
                        try(ResultSet k=p.getGeneratedKeys()){k.next();quizId=k.getInt(1);}
                    }
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO quiz_questions(quiz_id,question_id,position) SELECT ?,question_id,ROW_NUMBER() OVER(ORDER BY question_id) FROM questions WHERE category_id=?")){
                        p.setInt(1,quizId);p.setInt(2,catId);p.executeUpdate();
                    }
                }
            }
        }
    }

    private static String now() { return LocalDateTime.now().format(SQL_TIME); }
    private static void info(Frame f,String s){new InfoDialog(f,s,"Online Quiz");}
    private static void error(Frame f,Exception e){new InfoDialog(f,e.getMessage()==null?"Unexpected error":e.getMessage(),"Error");}
    private static void closeOnX(Window f){f.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){f.dispose();}});}
    private static void style(Frame f){f.setBackground(new Color(244,247,253));}

    private static class InfoDialog extends Dialog {
        InfoDialog(Frame owner,String message,String title){super(owner,title,true);setLayout(new BorderLayout(12,12));TextArea t=new TextArea(message,6,48,TextArea.SCROLLBARS_VERTICAL_ONLY);t.setEditable(false);add(t,BorderLayout.CENTER);Button b=new Button("OK");Panel p=new Panel();p.add(b);add(p,BorderLayout.SOUTH);b.addActionListener(e->dispose());closeOnX(this);setSize(460,210);setLocationRelativeTo(owner);setVisible(true);}
    }

    private static class AuthWindow extends Frame {
        TextField name=new TextField(22),email=new TextField(22),password=new TextField(22),confirm=new TextField(22),teacherCode=new TextField(22);
        Choice role=new Choice();
        AuthWindow(){
            super("Online Quiz Application");style(this);setLayout(new GridBagLayout());setSize(560,510);setLocationRelativeTo(null);
            password.setEchoChar('*');confirm.setEchoChar('*');teacherCode.setEchoChar('*');role.add("Student");role.add("Teacher");
            GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(7,8,7,8);g.fill=GridBagConstraints.HORIZONTAL;
            Label title=new Label("ONLINE QUIZ APPLICATION",Label.CENTER);title.setFont(new Font("Arial",Font.BOLD,22));title.setForeground(new Color(35,65,125));g.gridx=0;g.gridy=0;g.gridwidth=2;add(title,g);g.gridwidth=1;
            row(g,1,"Full name (registration)",name);row(g,2,"Email",email);row(g,3,"Password",password);row(g,4,"Confirm password",confirm);row(g,5,"Register as",role);row(g,6,"Teacher invitation code",teacherCode);
            Button login=new Button("Login"),register=new Button("Create account");Panel buttons=new Panel();buttons.add(login);buttons.add(register);g.gridx=0;g.gridy=7;g.gridwidth=2;add(buttons,g);
            Label hint=new Label("Teacher registration requires an invitation code.",Label.CENTER);g.gridy=8;add(hint,g);
            login.addActionListener(e->login());register.addActionListener(e->register());role.addItemListener(e->teacherCode.setEnabled("Teacher".equals(role.getSelectedItem())));
            teacherCode.setEnabled(false);closeOnX(this);setVisible(true);
        }
        void row(GridBagConstraints g,int y,String label,Component field){g.gridx=0;g.gridy=y;add(new Label(label),g);g.gridx=1;add(field,g);}
        void login(){
            try(Connection c=db();PreparedStatement p=c.prepareStatement("SELECT user_id,full_name,password_hash,role FROM users WHERE lower(email)=lower(?)")){
                p.setString(1,email.getText().trim());try(ResultSet r=p.executeQuery()){
                    if(!r.next()||!PasswordUtil.verify(password.getText(),r.getString("password_hash"))){info(this,"Invalid email or password.");return;}
                    new Dashboard(r.getInt(1),r.getString(2),r.getString(4));dispose();
                }
            }catch(Exception ex){error(this,ex);}
        }
        void register(){
            String n=name.getText().trim(),mail=email.getText().trim(),pw=password.getText();
            if(n.isEmpty()||!mail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")||pw.length()<8||!pw.equals(confirm.getText())){info(this,"Enter a valid name and email. Password must be at least 8 characters and both password fields must match.");return;}
            boolean teacher="Teacher".equals(role.getSelectedItem());
            if(teacher&&!TEACHER_CODE.equals(teacherCode.getText())){info(this,"Invalid teacher invitation code. Ask your project administrator for the code.");return;}
            String sql="INSERT INTO users(full_name,email,password_hash,role) VALUES(?,?,?,?)";
            try(Connection c=db();PreparedStatement p=c.prepareStatement(sql)){
                p.setString(1,n);p.setString(2,mail);p.setString(3,PasswordUtil.hash(pw));p.setString(4,teacher?"ADMIN":"STUDENT");p.executeUpdate();
                info(this,"Account created. You can now log in.");name.setText("");email.setText("");password.setText("");confirm.setText("");teacherCode.setText("");
            }catch(SQLException ex){if(ex.getMessage()!=null&&ex.getMessage().toLowerCase().contains("unique"))info(this,"This email is already registered.");else error(this,ex);}
        }
    }

    private static class QuizRow {
        int id,minutes,count;String title,subject,code,status,start,end;
        public String toString(){return title+" ["+subject+"] - "+status+" - "+count+" Qs";}
    }

    private static class Dashboard extends Frame {
        final int uid;final String name,role;final java.awt.List quizList=new java.awt.List(10);final java.util.List<QuizRow> rows=new ArrayList<>();
        Label stats=new Label("Loading your activity...");
        Dashboard(int id,String n,String r){super("Quiz Dashboard");uid=id;name=n;role=r;style(this);setSize(800,570);setLocationRelativeTo(null);setLayout(new BorderLayout(10,10));
            Panel top=new Panel(new GridLayout(2,1));Label welcome=new Label("Welcome, "+name+" ("+("ADMIN".equals(role)?"Teacher":"Student")+")",Label.CENTER);welcome.setFont(new Font("Arial",Font.BOLD,21));top.add(welcome);top.add(stats);add(top,BorderLayout.NORTH);
            Panel center=new Panel(new BorderLayout(8,8));center.add(new Label("Available / upcoming quizzes (select one):"),BorderLayout.NORTH);center.add(quizList,BorderLayout.CENTER);add(center,BorderLayout.CENTER);
            Panel buttons=new Panel(new FlowLayout());Button take=new Button("Take selected quiz"),join=new Button("Join by quiz code"),history=new Button("My results"),leader=new Button("Leaderboard"),refresh=new Button("Refresh"),logout=new Button("Logout");
            buttons.add(take);buttons.add(join);buttons.add(history);buttons.add(leader);buttons.add(refresh);if("ADMIN".equals(role))buttons.add(new Button("Teacher Console"));buttons.add(logout);add(buttons,BorderLayout.SOUTH);
            take.addActionListener(e->takeSelected());join.addActionListener(e->joinByCode());history.addActionListener(e->showHistory());leader.addActionListener(e->showLeaderboard());refresh.addActionListener(e->load());
            if("ADMIN".equals(role)){for(Component x:buttons.getComponents())if(x instanceof Button b&&b.getLabel().equals("Teacher Console"))b.addActionListener(e->new TeacherConsole(this,uid));}
            logout.addActionListener(e->{new AuthWindow();dispose();});quizList.addActionListener(e->takeSelected());closeOnX(this);load();setVisible(true);
        }
        void load(){quizList.removeAll();rows.clear();String sql="SELECT q.quiz_id,q.title,q.subject,q.duration_minutes,q.quiz_code,q.status,COALESCE(q.start_at,''),COALESCE(q.end_at,''),COUNT(qq.question_id) FROM quizzes q LEFT JOIN quiz_questions qq ON qq.quiz_id=q.quiz_id WHERE q.status='PUBLISHED' GROUP BY q.quiz_id ORDER BY q.created_at DESC";
            try(Connection c=db();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){while(r.next()){QuizRow x=new QuizRow();x.id=r.getInt(1);x.title=r.getString(2);x.subject=r.getString(3);x.minutes=r.getInt(4);x.code=r.getString(5);x.status=r.getString(6);x.start=r.getString(7);x.end=r.getString(8);x.count=r.getInt(9);rows.add(x);quizList.add(x.toString()+" | Code: "+x.code);}}
            catch(Exception ex){error(this,ex);}
            try(Connection c=db();PreparedStatement p=c.prepareStatement("SELECT COUNT(*),COALESCE(AVG(score*100.0/NULLIF(total_marks,0)),0),COALESCE(MAX(score*100.0/NULLIF(total_marks,0)),0) FROM attempts WHERE user_id=? AND status='SUBMITTED'")){p.setInt(1,uid);try(ResultSet r=p.executeQuery()){if(r.next())stats.setText("Completed attempts: "+r.getInt(1)+"    Average: "+String.format("%.1f%%",r.getDouble(2))+"    Best: "+String.format("%.1f%%",r.getDouble(3)));}}catch(Exception ex){error(this,ex);}
        }
        QuizRow selected(){int i=quizList.getSelectedIndex();return i<0||i>=rows.size()?null:rows.get(i);}
        void takeSelected(){QuizRow q=selected();if(q==null){info(this,"Select a quiz first.");return;}openQuiz(q);}
        void joinByCode(){TextField f=new TextField(18);Panel p=new Panel(new BorderLayout());p.add(new Label("Enter quiz code:"),BorderLayout.WEST);p.add(f,BorderLayout.CENTER);Dialog d=new Dialog(this,"Join quiz",true);d.add(p,BorderLayout.CENTER);Button b=new Button("Find quiz");d.add(b,BorderLayout.SOUTH);b.addActionListener(e->{String code=f.getText().trim();d.dispose();for(QuizRow q:rows)if(q.code.equalsIgnoreCase(code)){openQuiz(q);return;}info(this,"No published quiz found with that code.");});d.setSize(320,110);d.setLocationRelativeTo(this);d.setVisible(true);}
        void openQuiz(QuizRow q){if(q.count==0){info(this,"This quiz has no questions yet.");return;}if(!q.start.isEmpty()&&now().compareTo(q.start)<0){info(this,"This quiz is not open yet. Start time: "+q.start);return;}if(!q.end.isEmpty()&&now().compareTo(q.end)>0){info(this,"The quiz deadline has passed.");return;}new QuizWindow(this,uid,name,q);dispose();}
        void showHistory(){StringBuilder b=new StringBuilder("MY QUIZ HISTORY\n\n");try(Connection c=db();PreparedStatement p=c.prepareStatement("SELECT q.title,a.score,a.total_marks,a.correct_count,a.wrong_count,a.unanswered_count,a.submitted_at FROM attempts a JOIN quizzes q ON q.quiz_id=a.quiz_id WHERE a.user_id=? AND a.status='SUBMITTED' ORDER BY a.attempt_id DESC")){p.setInt(1,uid);try(ResultSet r=p.executeQuery()){while(r.next())b.append(r.getString(1)).append(" | ").append(r.getDouble(2)).append("/").append(r.getDouble(3)).append(" | Correct ").append(r.getInt(4)).append(", Wrong ").append(r.getInt(5)).append(", Unanswered ").append(r.getInt(6)).append(" | ").append(r.getString(7)).append("\n");}}catch(Exception ex){error(this,ex);return;}info(this,b.length()>16?b.toString():"No completed quizzes yet.");}
        void showLeaderboard(){StringBuilder b=new StringBuilder("TOP 10 LEADERBOARD (best percentage per student)\n\n");String sql="SELECT u.full_name,q.title,MAX(a.score*100.0/NULLIF(a.total_marks,0)) pct FROM attempts a JOIN users u ON u.user_id=a.user_id JOIN quizzes q ON q.quiz_id=a.quiz_id WHERE a.status='SUBMITTED' GROUP BY a.user_id,a.quiz_id ORDER BY pct DESC LIMIT 10";try(Connection c=db();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){int rank=1;while(r.next())b.append(rank++).append(". ").append(r.getString(1)).append(" - ").append(r.getString(2)).append(" - ").append(String.format("%.1f%%",r.getDouble(3))).append("\n");}catch(Exception ex){error(this,ex);return;}info(this,b.toString());}
    }

    private static class TeacherConsole extends Frame {
        final int teacherId;final Dashboard parent;final java.awt.List quizList=new java.awt.List(8);final java.util.List<Integer> ids=new ArrayList<>();
        TextField title=new TextField(25),subject=new TextField(25),duration=new TextField("10",5),marks=new TextField("1",5),start=new TextField(19),end=new TextField(19);
        TextField code=new TextField(12);TextArea description=new TextArea(2,25);Checkbox random=new Checkbox("Randomize questions",true),review=new Checkbox("Allow answer review",true);
        TeacherConsole(Dashboard p,int id){super("Teacher Console - Quiz Management");parent=p;teacherId=id;style(this);setSize(850,650);setLocationRelativeTo(p);setLayout(new BorderLayout(8,8));
            Panel create=new Panel(new GridLayout(0,2,5,5));create.add(new Label("Quiz title"));create.add(title);create.add(new Label("Subject"));create.add(subject);create.add(new Label("Duration (minutes)"));create.add(duration);create.add(new Label("Marks per question"));create.add(marks);create.add(new Label("Start time (yyyy-MM-dd HH:mm:ss, optional)"));create.add(start);create.add(new Label("End time (yyyy-MM-dd HH:mm:ss, optional)"));create.add(end);create.add(new Label("Quiz code (blank = auto)"));create.add(code);create.add(new Label("Description"));create.add(description);create.add(random);create.add(review);Button createQuiz=new Button("Create draft quiz");create.add(createQuiz);add(create,BorderLayout.NORTH);
            Panel middle=new Panel(new BorderLayout());middle.add(new Label("Your quizzes (select one to manage):"),BorderLayout.NORTH);middle.add(quizList,BorderLayout.CENTER);add(middle,BorderLayout.CENTER);
            Panel actions=new Panel();Button addQ=new Button("Add question"),viewQ=new Button("View / delete questions"),publish=new Button("Publish / close"),report=new Button("Quiz report"),close=new Button("Close");actions.add(addQ);actions.add(viewQ);actions.add(publish);actions.add(report);actions.add(close);add(actions,BorderLayout.SOUTH);
            createQuiz.addActionListener(e->createQuiz());addQ.addActionListener(e->addQuestion());viewQ.addActionListener(e->manageQuestions());publish.addActionListener(e->changeStatus());report.addActionListener(e->report());close.addActionListener(e->dispose());quizList.addActionListener(e->{});closeOnX(this);load();setVisible(true);
        }
        void load(){quizList.removeAll();ids.clear();try(Connection c=db();PreparedStatement p=c.prepareStatement("SELECT quiz_id,title,subject,status,quiz_code FROM quizzes WHERE teacher_id=? ORDER BY quiz_id DESC")){p.setInt(1,teacherId);try(ResultSet r=p.executeQuery()){while(r.next()){ids.add(r.getInt(1));quizList.add(r.getString(2)+" | "+r.getString(3)+" | "+r.getString(4)+" | "+r.getString(5));}}}catch(Exception ex){error(this,ex);}}
        int selected(){int i=quizList.getSelectedIndex();return i<0?-1:ids.get(i);}
        void createQuiz(){try{int mins=Integer.parseInt(duration.getText().trim());double mpq=Double.parseDouble(marks.getText().trim());if(title.getText().trim().isEmpty()||subject.getText().trim().isEmpty()||mins<1||mins>240||mpq<=0)throw new IllegalArgumentException("Enter title, subject, valid duration (1-240) and positive marks.");String qcode=code.getText().trim();if(qcode.isEmpty())qcode="QZ"+UUID.randomUUID().toString().substring(0,6).toUpperCase(Locale.ROOT);String sql="INSERT INTO quizzes(title,subject,description,duration_minutes,marks_per_question,quiz_code,teacher_id,status,start_at,end_at,randomize,allow_review) VALUES(?,?,?,?,?,?,?,'DRAFT',?,?,?,?)";try(Connection c=db();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,title.getText().trim());p.setString(2,subject.getText().trim());p.setString(3,description.getText().trim());p.setInt(4,mins);p.setDouble(5,mpq);p.setString(6,qcode);p.setInt(7,teacherId);p.setString(8,start.getText().trim().isEmpty()?null:start.getText().trim());p.setString(9,end.getText().trim().isEmpty()?null:end.getText().trim());p.setInt(10,random.getState()?1:0);p.setInt(11,review.getState()?1:0);p.executeUpdate();}info(this,"Draft quiz created. Add questions, then publish it. Quiz code: "+qcode);load();}catch(Exception ex){error(this,ex);}}
        void addQuestion(){int id=selected();if(id<0){info(this,"Select a quiz first.");return;}new QuestionDialog(this,id);}
        void manageQuestions(){int id=selected();if(id<0){info(this,"Select a quiz first.");return;}StringBuilder b=new StringBuilder();java.util.List<Integer> qids=new ArrayList<>();try(Connection c=db();PreparedStatement p=c.prepareStatement("SELECT q.question_id,q.question_text FROM quiz_questions x JOIN questions q ON q.question_id=x.question_id WHERE x.quiz_id=? ORDER BY x.position")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){while(r.next()){qids.add(r.getInt(1));b.append(r.getInt(1)).append(" - ").append(r.getString(2)).append("\n");}}}catch(Exception ex){error(this,ex);return;}TextField del=new TextField(8);Dialog d=new Dialog(this,"Quiz questions",true);d.setLayout(new BorderLayout());TextArea list=new TextArea(b.toString(),12,55,TextArea.SCROLLBARS_VERTICAL_ONLY);list.setEditable(false);d.add(list,BorderLayout.CENTER);Panel bottom=new Panel();bottom.add(new Label("Question ID to remove:"));bottom.add(del);Button remove=new Button("Remove"),done=new Button("Close");bottom.add(remove);bottom.add(done);d.add(bottom,BorderLayout.SOUTH);remove.addActionListener(e->{try(Connection c=db();PreparedStatement p=c.prepareStatement("DELETE FROM quiz_questions WHERE quiz_id=? AND question_id=?")){p.setInt(1,id);p.setInt(2,Integer.parseInt(del.getText().trim()));if(p.executeUpdate()==0)info(this,"Question was not found in this quiz.");else{info(this,"Question removed from this quiz.");d.dispose();manageQuestions();}}catch(Exception ex){error(this,ex);}});done.addActionListener(e->d.dispose());d.setSize(560,330);d.setLocationRelativeTo(this);d.setVisible(true);}
        void changeStatus(){int id=selected();if(id<0){info(this,"Select a quiz first.");return;}try(Connection c=db();PreparedStatement count=db().prepareStatement("SELECT COUNT(*) FROM quiz_questions WHERE quiz_id=?")){count.setInt(1,id);try(ResultSet r=count.executeQuery()){r.next();if(r.getInt(1)==0){info(this,"Add at least one question before publishing.");return;}}}catch(Exception ex){error(this,ex);return;}try(Connection c=db();PreparedStatement p=c.prepareStatement("UPDATE quizzes SET status=CASE WHEN status='PUBLISHED' THEN 'CLOSED' ELSE 'PUBLISHED' END WHERE quiz_id=? AND teacher_id=?")){p.setInt(1,id);p.setInt(2,teacherId);p.executeUpdate();load();}catch(Exception ex){error(this,ex);}}
        void report(){int id=selected();if(id<0){info(this,"Select a quiz first.");return;}String sql="SELECT COUNT(*),COALESCE(AVG(score*100.0/NULLIF(total_marks,0)),0),COALESCE(MAX(score*100.0/NULLIF(total_marks,0)),0) FROM attempts WHERE quiz_id=? AND status='SUBMITTED'";try(Connection c=db();PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,id);try(ResultSet r=p.executeQuery()){r.next();info(this,"Quiz report\nAttempts: "+r.getInt(1)+"\nAverage score: "+String.format("%.1f%%",r.getDouble(2))+"\nHighest score: "+String.format("%.1f%%",r.getDouble(3)));}}catch(Exception ex){error(this,ex);}}
    }

    private static class QuestionDialog extends Dialog {
        final int quizId;TextArea text=new TextArea(3,42);TextField[] opts={new TextField(32),new TextField(32),new TextField(32),new TextField(32)};Choice answer=new Choice();
        QuestionDialog(Frame owner,int qid){super(owner,"Add MCQ",true);quizId=qid;setLayout(new GridBagLayout());GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(5,6,5,6);g.fill=GridBagConstraints.HORIZONTAL;row(g,0,"Question",text);for(int i=0;i<4;i++)row(g,i+1,"Option "+(char)('A'+i),opts[i]);for(String s:new String[]{"A","B","C","D"})answer.add(s);row(g,5,"Correct answer",answer);Button save=new Button("Save question");g.gridx=1;g.gridy=6;add(save,g);save.addActionListener(e->save());closeOnX(this);setSize(560,430);setLocationRelativeTo(owner);setVisible(true);}
        void row(GridBagConstraints g,int y,String l,Component f){g.gridx=0;g.gridy=y;add(new Label(l),g);g.gridx=1;add(f,g);}
        void save(){if(text.getText().trim().isEmpty()){info((Frame)getOwner(),"Enter question text.");return;}for(TextField f:opts)if(f.getText().trim().isEmpty()){info((Frame)getOwner(),"Fill all four options.");return;}try(Connection c=db()){c.setAutoCommit(false);int categoryId;try(PreparedStatement p=c.prepareStatement("INSERT OR IGNORE INTO categories(category_name) VALUES(?)")){p.setString(1,"Custom");p.executeUpdate();}try(PreparedStatement p=c.prepareStatement("SELECT category_id FROM categories WHERE category_name='Custom'");ResultSet r=p.executeQuery()){r.next();categoryId=r.getInt(1);}int qid;try(PreparedStatement p=c.prepareStatement("INSERT INTO questions(category_id,question_text,option_a,option_b,option_c,option_d,correct_option) VALUES(?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){p.setInt(1,categoryId);p.setString(2,text.getText().trim());for(int i=0;i<4;i++)p.setString(i+3,opts[i].getText().trim());p.setString(7,answer.getSelectedItem());p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){r.next();qid=r.getInt(1);}}try(PreparedStatement p=c.prepareStatement("INSERT INTO quiz_questions(quiz_id,question_id,position) VALUES(?,?,COALESCE((SELECT MAX(position)+1 FROM quiz_questions WHERE quiz_id=?),0))")){p.setInt(1,quizId);p.setInt(2,qid);p.setInt(3,quizId);p.executeUpdate();}c.commit();info((Frame)getOwner(),"Question added to quiz.");dispose();}catch(Exception ex){error((Frame)getOwner(),ex);}}
    }

    private static class Q {
        int id;String text,a,b,c,d,correct;
        String option(String x){return switch(x){case "A"->a;case "B"->b;case "C"->c;case "D"->d;default->"";};}
    }

    private static class QuizWindow extends Frame {
        final Dashboard parent;final int uid;final String userName,userRole;final QuizRow quiz;final java.util.List<Q> questions=new ArrayList<>();
        final Map<Integer,String> answers=new HashMap<>();final Set<Integer> review=new HashSet<>();
        int index=0,seconds;int attemptId;boolean finished=false;final Label timer=new Label();final Label qlabel=new Label("",Label.LEFT);final CheckboxGroup group=new CheckboxGroup();final Checkbox[] choices=new Checkbox[4];final Label progress=new Label();
        QuizWindow(Dashboard p,int user,String n,QuizRow q){super("Quiz - "+q.title);parent=p;uid=user;userName=n;userRole=p.role;quiz=q;seconds=q.minutes*60;style(this);setSize(780,500);setLocationRelativeTo(null);setLayout(new BorderLayout(12,12));
            Panel top=new Panel(new BorderLayout());Label heading=new Label(q.title+" | "+q.subject);heading.setFont(new Font("Arial",Font.BOLD,18));top.add(heading,BorderLayout.WEST);timer.setFont(new Font("Arial",Font.BOLD,18));top.add(timer,BorderLayout.EAST);add(top,BorderLayout.NORTH);
            Panel center=new Panel(new GridLayout(5,1,6,8));qlabel.setFont(new Font("Arial",Font.BOLD,16));center.add(qlabel);for(int i=0;i<4;i++){choices[i]=new Checkbox("",group,false);choices[i].setFont(new Font("Arial",Font.PLAIN,15));center.add(choices[i]);}add(center,BorderLayout.CENTER);
            Panel bottom=new Panel(new FlowLayout());Button previous=new Button("Previous"),next=new Button("Next"),mark=new Button("Mark for review"),clear=new Button("Clear answer"),submit=new Button("Submit");bottom.add(progress);bottom.add(previous);bottom.add(next);bottom.add(mark);bottom.add(clear);bottom.add(submit);add(bottom,BorderLayout.SOUTH);
            previous.addActionListener(e->{save();if(index>0){index--;showQ();}});next.addActionListener(e->{save();if(index<questions.size()-1){index++;showQ();}});mark.addActionListener(e->{save();int id=questions.get(index).id;if(!review.add(id))review.remove(id);showQ();});clear.addActionListener(e->{group.setSelectedCheckbox(null);answers.remove(questions.get(index).id);});submit.addActionListener(e->finish(false));
            closeOnX(this);loadQuestions();if(questions.isEmpty()){info(this,"This quiz has no questions.");dispose();return;}startAttempt();showQ();setVisible(true);updateTimer();Thread t=new Thread(()->{while(!finished&&seconds>0){try{Thread.sleep(1000);}catch(InterruptedException e){return;}seconds--;EventQueue.invokeLater(this::updateTimer);}if(!finished)EventQueue.invokeLater(()->finish(true));},"quiz-timer");t.setDaemon(true);t.start();
        }
        void loadQuestions(){String sql="SELECT q.question_id,q.question_text,q.option_a,q.option_b,q.option_c,q.option_d,q.correct_option FROM quiz_questions x JOIN questions q ON q.question_id=x.question_id WHERE x.quiz_id=? ORDER BY "+(isRandom()?"RANDOM()":"x.position");try(Connection c=db();PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,quiz.id);try(ResultSet r=p.executeQuery()){while(r.next()){Q x=new Q();x.id=r.getInt(1);x.text=r.getString(2);x.a=r.getString(3);x.b=r.getString(4);x.c=r.getString(5);x.d=r.getString(6);x.correct=r.getString(7);questions.add(x);}}}catch(Exception ex){error(this,ex);}}
        boolean isRandom(){try(Connection c=db();PreparedStatement p=c.prepareStatement("SELECT randomize FROM quizzes WHERE quiz_id=?")){p.setInt(1,quiz.id);try(ResultSet r=p.executeQuery()){return r.next()&&r.getInt(1)==1;}}catch(Exception e){return false;}}
        void startAttempt(){try(Connection c=db();PreparedStatement p=c.prepareStatement("INSERT INTO attempts(quiz_id,user_id,total_marks,status) VALUES(?,?,?,'IN_PROGRESS')",Statement.RETURN_GENERATED_KEYS)){p.setInt(1,quiz.id);p.setInt(2,uid);p.setDouble(3,questions.size()*getMarks());p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){r.next();attemptId=r.getInt(1);}}catch(Exception ex){error(this,ex);}}
        double getMarks(){try(Connection c=db();PreparedStatement p=c.prepareStatement("SELECT marks_per_question FROM quizzes WHERE quiz_id=?")){p.setInt(1,quiz.id);try(ResultSet r=p.executeQuery()){return r.next()?r.getDouble(1):1;}}catch(Exception e){return 1;}}
        void save(){Q q=questions.get(index);for(int i=0;i<4;i++)if(choices[i].getState()){answers.put(q.id,String.valueOf((char)('A'+i)));return;}answers.remove(q.id);}
        void showQ(){Q q=questions.get(index);qlabel.setText("Question "+(index+1)+" of "+questions.size()+": "+q.text+(review.contains(q.id)?"  [MARKED FOR REVIEW]":""));String[] v={q.a,q.b,q.c,q.d};group.setSelectedCheckbox(null);for(int i=0;i<4;i++){choices[i].setLabel((char)('A'+i)+". "+v[i]);choices[i].setState(Objects.equals(answers.get(q.id),String.valueOf((char)('A'+i))));}progress.setText("Answered: "+answers.size()+" | Review: "+review.size());}
        void updateTimer(){timer.setText(String.format("Time left: %02d:%02d",seconds/60,seconds%60));}
        void finish(boolean timeout){if(finished)return;if(!timeout)save();if(!timeout&&seconds>0){if(!"yes".equals(System.getProperty("quiz.skipConfirm"))){Dialog d=new Dialog(this,"Submit quiz?",true);d.setLayout(new FlowLayout());d.add(new Label("Are you sure you want to submit?"));Button yes=new Button("Submit"),no=new Button("Continue");d.add(yes);d.add(no);yes.addActionListener(e->{d.dispose();complete();});no.addActionListener(e->d.dispose());d.setSize(300,120);d.setLocationRelativeTo(this);d.setVisible(true);return;}}complete();}
        void complete(){if(finished)return;finished=true;int correct=0,wrong=0,unanswered=0;double marks=getMarks(),score=0;try(Connection c=db()){c.setAutoCommit(false);for(Q q:questions){String ans=answers.get(q.id);boolean ok=q.correct.equals(ans);if(ans==null)unanswered++;else if(ok){correct++;score+=marks;}else wrong++;try(PreparedStatement p=c.prepareStatement("INSERT OR REPLACE INTO student_answers(attempt_id,question_id,selected_option,is_correct,marked_review) VALUES(?,?,?,?,?)")){p.setInt(1,attemptId);p.setInt(2,q.id);p.setString(3,ans);p.setInt(4,ok?1:0);p.setInt(5,review.contains(q.id)?1:0);p.executeUpdate();}}
                try(PreparedStatement p=c.prepareStatement("UPDATE attempts SET submitted_at=?,score=?,correct_count=?,wrong_count=?,unanswered_count=?,status='SUBMITTED' WHERE attempt_id=?")){p.setString(1,now());p.setDouble(2,score);p.setInt(3,correct);p.setInt(4,wrong);p.setInt(5,unanswered);p.setInt(6,attemptId);p.executeUpdate();}c.commit();
                double total=questions.size()*marks;double pct=total==0?0:score*100/total;info(this,String.format("QUIZ SUBMITTED%s\n\nScore: %.2f / %.2f (%.1f%%)\nCorrect: %d\nWrong: %d\nUnanswered: %d\nTime used: %d seconds",timeout?" (time expired)":"",score,total,pct,correct,wrong,unanswered,quiz.minutes*60-seconds));
            }catch(Exception ex){error(this,ex);}dispose();new Dashboard(uid,userName,userRole);}
    }
}
