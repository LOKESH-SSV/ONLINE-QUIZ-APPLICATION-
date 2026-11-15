# Online Quiz Application – Java AWT + SQLite (VS Code)

A desktop-based multiple-choice quiz application built with **Java AWT**, **JDBC** and **SQLite**. It uses no Swing components and does not require a MySQL server.

## Features
- Registration lets the user choose **Student** or **Teacher**.
- Login redirects both account types to the dashboard with the correct permissions.
- Teacher dashboard includes question management: add MCQs by category and choose the correct option.
- Student dashboard supports subject/category-based quizzes.
- Four options per question, next-question navigation and answer retention.
- Ten-minute countdown with automatic submission.
- Automatic scoring, result storage and top-10 leaderboard.
- Password hashing using PBKDF2-HMAC-SHA256.
- SQLite database and sample questions are initialized automatically.

## Requirements
- JDK 17 or later
- VS Code
- VS Code extension: **Extension Pack for Java** (Microsoft)
- Maven 3.8 or later

## Open and run in VS Code
1. Extract this ZIP file.
2. In VS Code, choose **File → Open Folder** and select `OnlineQuizApplication_AWT_SQLite`.
3. Install the **Extension Pack for Java** if VS Code prompts you.
4. Open **Terminal → New Terminal**.
5. Run `mvn clean compile`.
6. Run `mvn exec:java -Dexec.mainClass="com.collegequiz.Main"`.
7. Maven downloads the SQLite JDBC driver automatically. The app creates `online_quiz.db` in the project working directory.

You can also open `Main.java` and use the Java extension's Run option after Maven has resolved the project dependencies.

## How registration and roles work
1. Enter your name, email and password (minimum 6 characters).
2. In **Register as**, select **Student** or **Teacher**.
3. Click **Create Account**.
4. Log in with the same email and password.
5. Students can take quizzes. Teachers see the **Manage Questions** button and can add questions.

For this college demonstration build, teacher self-registration is enabled so you can demonstrate both roles easily. In a real deployment, teacher accounts should be approved by an administrator or protected by an institution-issued invitation code; otherwise a student could select Teacher.

## Database
SQLite file: `online_quiz.db`

Tables:
- `users`: account details and role (`STUDENT` or internal teacher role `ADMIN`)
- `categories`: subject/category names
- `questions`: MCQ text, four options and correct answer
- `results`: quiz scores and attempt details

The internal value `ADMIN` is retained for teacher accounts to remain compatible with the existing database schema and teacher-management permission checks. The registration screen displays the friendly label **Teacher**.

## Source files
- `Main.java`: application entry point and database initialization
- `DB.java`: SQLite connection, table creation and sample data
- `PasswordUtil.java`: password hashing and verification
- `Question.java`: question model
- `Msg.java`: AWT message dialog helper
- `AuthFrame.java`: login, registration and Student/Teacher selection
- `DashboardFrame.java`: role-aware dashboard, category selection and leaderboard
- `QuizFrame.java`: quiz, timer, scoring and result saving
- `AdminFrame.java`: teacher question-entry screen

## Team division
See `Team_Division.md` for the three-member work split.

## Notes
- The interface uses Java AWT only; no Swing widgets are used.
- SQLite is an embedded database; no separate database server is needed.
- This is a local desktop prototype, not a hosted multi-user web application.
