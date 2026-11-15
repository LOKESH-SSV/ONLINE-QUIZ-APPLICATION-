# Online Quiz Application — Java AWT + SQLite

A desktop MCQ examination system built with Java 17, Java AWT, JDBC and SQLite. No Swing or external database server is required.

## Implemented features
- Student and Teacher registration/login with PBKDF2-HMAC-SHA256 password hashing.
- Separate role-aware dashboard actions.
- Teacher question bank: add, select, edit and delete questions by subject.
- Teacher quiz creation: title, subject, duration, question count and optional access code.
- Publish and close quizzes; generated access codes can be shared with students.
- Student published-quiz list and join-by-code workflow.
- Timed quizzes with previous/next navigation, answer retention and auto-submit on timeout.
- Automatic scoring, persistent result history and leaderboard.
- Teacher analytics showing subject attempts, unique participants, average/best score and recent results.
- SQLite schema initialization and sample MCQs.

## Requirements
- JDK 17+
- Maven 3.8+
- VS Code with Extension Pack for Java (recommended)

## Run
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.collegequiz.Main"
```
The application creates `online_quiz.db` in the project working directory.

## Roles and security note
The registration screen offers Student or Teacher. The teacher role is stored internally as `ADMIN` for compatibility with the original schema. For a real deployment, do not allow unrestricted teacher self-registration: use an administrator approval or institution-issued invitation code. This repository is a local desktop academic prototype; it is not a hosted multi-user examination service.

## Main source files
- `Main.java`: entry point
- `DB.java`: SQLite schema, migration-safe table creation and sample data
- `AuthFrame.java`: registration and login
- `DashboardFrame.java`: role-aware navigation and practice quizzes
- `AdminFrame.java`: teacher question-bank management
- `QuizManagementFrame.java`: create, publish and close quizzes
- `AvailableQuizzesFrame.java`: student quiz discovery and access-code join
- `QuizFrame.java`: timed quiz, grading and answer persistence
- `HistoryFrame.java`: student result history
- `AnalyticsFrame.java`: teacher performance summary

## Database
SQLite tables: `users`, `categories`, `questions`, `results`, `quizzes`, `quiz_questions`, `attempts`, `student_answers`.

## Important testing notes
- Existing databases are upgraded by `CREATE TABLE IF NOT EXISTS`; no existing user rows are intentionally deleted.
- Create a teacher account, add questions, publish a quiz, then log in as a student and use its access code.
- A published quiz uses a snapshot list of question IDs. If a teacher edits or deletes a question after publishing, the quiz's question bank may change; publish only after reviewing the questions.
- The desktop app stores data locally in SQLite. Do not commit real student personal data or production database files to a public repository.
