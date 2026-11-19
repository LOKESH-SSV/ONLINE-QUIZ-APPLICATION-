# Online Quiz Application — Redesign

The redesign is being developed on the `redesign/fresh-database-jdbc-ui` branch. The existing `main` branch is not overwritten.

## Fresh database

The new application uses `online_quiz_v2.db`, separate from the legacy `online_quiz.db`. It starts empty and does not import legacy records. The database schema is versioned in `src/main/resources/db/migration`.

## Database tables

- `users`: student and teacher accounts
- `subjects`: reusable subject catalog
- `quizzes`: teacher-owned quiz settings, scheduling, access code and optional negative marking
- `questions`: reusable teacher-owned question bank
- `quiz_options`: four answer choices and the correct choice
- `quiz_questions`: ordered questions and per-question marks for each quiz
- `attempts`: one student's attempt and scoring summary
- `student_answers`: selected option and review state for each question
- `results`: published result linked to its attempt
- `audit_logs`: important application actions
- `schema_history`: applied schema migration versions

## JDBC rules

- All SQL belongs in DAO/repository classes, not AWT event handlers.
- Use prepared statements for values supplied by users.
- Use try-with-resources for connections, statements and result sets.
- Enable SQLite foreign keys and a busy timeout on every connection.
- Use transactions for quiz submission and result persistence.
- Keep database access off the AWT event-dispatch thread for longer operations.

## Planned application layers

```text
ui/          AWT windows and reusable UI components
model/       Plain Java domain objects
dao/         Prepared-statement database operations
service/     Authentication, quiz rules and scoring
database/    Connection management and migrations
```

## Important

The fresh database file is separate from the old database. Existing records are intentionally not migrated, per the project owner's choice. The new schema and JDBC foundation are the first implementation stage; the application UI and services must be switched to this foundation before the redesign is considered complete.
