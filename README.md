# Online Quiz Application — Java AWT + SQLite

A desktop-based quiz and examination management system built with Java 17, AWT, JDBC and SQLite.

## Features
### Accounts and access
- Student and Teacher registration and login.
- PBKDF2-HMAC-SHA256 password hashing.
- Teacher registration protected by an invitation code.
- Role-based dashboards and teacher-only quiz management.

### Teacher
- Create quizzes with title, subject, description, duration, marks per question and optional start/end time.
- Configure random question order and whether answer review is allowed.
- Add MCQs with four options and a correct answer.
- Remove questions from a quiz.
- Publish or close quizzes and share a unique quiz code.
- View attempt count, average and highest score report.

### Student
- View published quizzes and join by quiz code.
- Take timed quizzes with previous/next navigation.
- Save answers, clear answers and mark questions for review.
- Automatic submission when the timer expires.
- Automatic evaluation with correct, wrong and unanswered counts.
- View personal result history, average/best score and leaderboard.

### Technical
- Java AWT only (no Swing).
- SQLite database created and upgraded automatically.
- JDBC prepared statements and foreign-key enforcement.
- Sample subject quizzes are created automatically on first run.

## Requirements
- JDK 17 or later
- Maven 3.8+
- VS Code with Extension Pack for Java (optional)

## Run
Open the repository folder in VS Code, then run:

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.collegequiz.Main"
```

The database file `online_quiz.db` is created in the project working directory.

## Teacher invitation code
For this college demonstration, teacher self-registration requires an invitation code. The default local demo code is:

```
QUIZ-TEACHER-2026
```

For a different code, launch with a JVM property:

```bash
mvn exec:java -Dexec.mainClass="com.collegequiz.Main" -Dquiz.teacherCode="YOUR-PRIVATE-CODE"
```

Do not use the demo code in a real deployment. Change it before sharing the application.

## Demo account
On first launch, the app creates a demo teacher account if no teacher exists:
- Email: `demo.teacher@quiz.local`
- Password: `Teacher@123`

Change or remove this account before real use. Students can register from the login screen.

## Database
The application initializes the original tables (`users`, `categories`, `questions`, `results`) and adds:
- `quizzes`
- `quiz_questions`
- `attempts`
- `student_answers`

Existing database files are retained; schema changes use `CREATE TABLE IF NOT EXISTS`.

## Project structure
- `Main.java`: application entry point
- `App.java`: login/registration, student dashboard, teacher console, quiz runner, results and leaderboard
- `DB.java`: SQLite connection and original schema/sample questions
- `PasswordUtil.java`: password hashing and verification
- `Question.java`: legacy question model
- `AuthFrame.java`, `DashboardFrame.java`, `AdminFrame.java`, `QuizFrame.java`: original UI classes retained for compatibility

## Important project limitations
This is a local desktop prototype. It is not a hosted web service and does not yet provide server-side proctoring, concurrent remote exam sessions, email verification, PDF export, CSV import, or a full question-bank edit screen. Teacher quiz reports are aggregate reports, not institutional-grade analytics. SQLite is intended for a small classroom demonstration.
