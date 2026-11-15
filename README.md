# Online Quiz Application — Java AWT + SQLite

A desktop online-examination prototype built with **Java 17, AWT, JDBC, Maven and SQLite**. It supports teacher-created quizzes, reusable question-bank content, timed student attempts, result analysis and teacher reports.

## Implemented features

### Student
- Student registration and login with PBKDF2-HMAC-SHA256 password hashing.
- Dashboard with completed quiz count, average score and best score.
- Available published quizzes and access-code joining.
- Quiz schedule enforcement using optional start and end date/time.
- Timed exam with previous/next navigation and a question-number palette.
- Mark questions for review and identify answered/unanswered questions.
- Automatic answer saving while attempting a quiz.
- Automatic submission when the timer reaches zero.
- Result history with correct, wrong, unanswered, negative marks, final score and time taken.
- Topic-wise performance and leaderboard.

### Teacher
- Teacher registration/login (stored as role `ADMIN` for compatibility).
- Create quizzes with title, subject, duration, question count and access code.
- Enter new questions during quiz creation or select existing questions from the question bank.
- Question bank supports add, edit, delete, topic and difficulty (Easy/Medium/Hard).
- Questions already used in a quiz are locked against edits/deletion so past exams are not changed.
- Publish or close quizzes.
- Optional quiz scheduling with start and end time.
- **Optional negative marking per quiz**: teacher can enable it and set the marks deducted for each wrong answer. It is disabled by default.
- Teacher analytics: attempts, unique students, average/best score, detailed attempt results and question-bank summary.
- Export teacher reports as CSV or PDF.

## Scoring rules

- Correct answer: **+1 mark**.
- Wrong answer: **0 marks** when negative marking is disabled.
- Wrong answer: **minus the teacher-configured penalty** when negative marking is enabled.
- Unanswered question: **0 marks**.
- Final raw score = correct answers − (wrong answers × penalty).
- Percentage = (raw score ÷ total questions) × 100.

The negative-marking setting and penalty are stored with each quiz. They do not change when a teacher creates another quiz.

## Requirements
- JDK 17+
- Maven 3.8+
- VS Code with Extension Pack for Java (recommended)

## Run
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.collegequiz.Main"
```

The application creates or upgrades `online_quiz.db` in the project working directory. Database updates are additive; existing users and results are retained.

## Suggested demonstration flow
1. Register/login as a teacher.
2. Open Question Bank and add questions with topics and difficulty levels.
3. Create a quiz. Set its schedule if needed.
4. Enable negative marking if required and enter the penalty per wrong answer.
5. Choose existing bank questions or enter new questions.
6. Share the generated access code with a student.
7. Login as a student and join the quiz.
8. Answer questions, navigate using the palette, and mark questions for review.
9. Submit the quiz or allow the timer to auto-submit.
10. View student performance and teacher analytics; export CSV/PDF reports.

## Main source files
- `Main.java`: application entry point.
- `DB.java`: SQLite schema creation and additive migrations.
- `AuthFrame.java`: registration and login.
- `DashboardFrame.java`: student/teacher dashboard.
- `AdminFrame.java`: reusable question bank.
- `QuizManagementFrame.java`: quiz creation, scheduling and negative-marking settings.
- `AvailableQuizzesFrame.java`: quiz discovery, schedule checks and access-code joining.
- `QuizFrame.java`: timed exam, navigation, answer autosave, review marks and scoring.
- `HistoryFrame.java`: student result history and topic-wise analysis.
- `AnalyticsFrame.java`: teacher analytics.
- `ReportExporter.java`: CSV and PDF report export.

## Database tables
`users`, `categories`, `questions`, `results`, `quizzes`, `quiz_questions`, `attempts`, and `student_answers`.

## Academic/demo note
This is a local desktop academic prototype, not a hosted multi-user examination service. For a real deployment, teacher accounts should require administrator approval or an institution-issued invitation code. Do not commit real student personal data or production database files to a public repository.
