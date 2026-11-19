PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL CHECK(length(trim(full_name)) >= 2),
    email TEXT NOT NULL COLLATE NOCASE UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('STUDENT', 'TEACHER')),
    is_active INTEGER NOT NULL DEFAULT 1 CHECK(is_active IN (0, 1)),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subjects (
    subject_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL COLLATE NOCASE UNIQUE,
    description TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS quizzes (
    quiz_id INTEGER PRIMARY KEY AUTOINCREMENT,
    teacher_id INTEGER NOT NULL,
    subject_id INTEGER,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    access_code TEXT NOT NULL COLLATE NOCASE UNIQUE,
    duration_minutes INTEGER NOT NULL CHECK(duration_minutes BETWEEN 1 AND 600),
    status TEXT NOT NULL DEFAULT 'DRAFT'
        CHECK(status IN ('DRAFT', 'PUBLISHED', 'CLOSED')),
    shuffle_questions INTEGER NOT NULL DEFAULT 0 CHECK(shuffle_questions IN (0, 1)),
    shuffle_options INTEGER NOT NULL DEFAULT 0 CHECK(shuffle_options IN (0, 1)),
    allow_review INTEGER NOT NULL DEFAULT 1 CHECK(allow_review IN (0, 1)),
    negative_marking_enabled INTEGER NOT NULL DEFAULT 0
        CHECK(negative_marking_enabled IN (0, 1)),
    negative_marks REAL NOT NULL DEFAULT 0 CHECK(negative_marks >= 0),
    starts_at TEXT,
    ends_at TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK(negative_marking_enabled = 1 OR negative_marks = 0),
    CHECK(starts_at IS NULL OR ends_at IS NULL OR starts_at < ends_at),
    FOREIGN KEY(teacher_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    FOREIGN KEY(subject_id) REFERENCES subjects(subject_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS questions (
    question_id INTEGER PRIMARY KEY AUTOINCREMENT,
    teacher_id INTEGER NOT NULL,
    subject_id INTEGER,
    question_text TEXT NOT NULL,
    explanation TEXT NOT NULL DEFAULT '',
    difficulty TEXT NOT NULL DEFAULT 'MEDIUM'
        CHECK(difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(teacher_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY(subject_id) REFERENCES subjects(subject_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS quiz_options (
    option_id INTEGER PRIMARY KEY AUTOINCREMENT,
    question_id INTEGER NOT NULL,
    option_key TEXT NOT NULL CHECK(option_key IN ('A', 'B', 'C', 'D')),
    option_text TEXT NOT NULL,
    is_correct INTEGER NOT NULL DEFAULT 0 CHECK(is_correct IN (0, 1)),
    UNIQUE(question_id, option_key),
    FOREIGN KEY(question_id) REFERENCES questions(question_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS quiz_questions (
    quiz_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    position INTEGER NOT NULL DEFAULT 1 CHECK(position > 0),
    marks REAL NOT NULL DEFAULT 1 CHECK(marks >= 0),
    PRIMARY KEY(quiz_id, question_id),
    UNIQUE(quiz_id, position),
    FOREIGN KEY(quiz_id) REFERENCES quizzes(quiz_id) ON DELETE CASCADE,
    FOREIGN KEY(question_id) REFERENCES questions(question_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS attempts (
    attempt_id INTEGER PRIMARY KEY AUTOINCREMENT,
    quiz_id INTEGER NOT NULL,
    student_id INTEGER NOT NULL,
    started_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TEXT,
    status TEXT NOT NULL DEFAULT 'IN_PROGRESS'
        CHECK(status IN ('IN_PROGRESS', 'SUBMITTED', 'AUTO_SUBMITTED', 'CANCELLED')),
    total_questions INTEGER NOT NULL DEFAULT 0 CHECK(total_questions >= 0),
    answered_count INTEGER NOT NULL DEFAULT 0 CHECK(answered_count >= 0),
    correct_count INTEGER NOT NULL DEFAULT 0 CHECK(correct_count >= 0),
    wrong_count INTEGER NOT NULL DEFAULT 0 CHECK(wrong_count >= 0),
    unanswered_count INTEGER NOT NULL DEFAULT 0 CHECK(unanswered_count >= 0),
    raw_score REAL NOT NULL DEFAULT 0,
    penalty REAL NOT NULL DEFAULT 0 CHECK(penalty >= 0),
    final_score REAL NOT NULL DEFAULT 0,
    time_taken_seconds INTEGER NOT NULL DEFAULT 0 CHECK(time_taken_seconds >= 0),
    FOREIGN KEY(quiz_id) REFERENCES quizzes(quiz_id) ON DELETE RESTRICT,
    FOREIGN KEY(student_id) REFERENCES users(user_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS student_answers (
    answer_id INTEGER PRIMARY KEY AUTOINCREMENT,
    attempt_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    selected_option_id INTEGER,
    is_correct INTEGER NOT NULL DEFAULT 0 CHECK(is_correct IN (0, 1)),
    marked_for_review INTEGER NOT NULL DEFAULT 0 CHECK(marked_for_review IN (0, 1)),
    answered_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(attempt_id, question_id),
    FOREIGN KEY(attempt_id) REFERENCES attempts(attempt_id) ON DELETE CASCADE,
    FOREIGN KEY(question_id) REFERENCES questions(question_id) ON DELETE RESTRICT,
    FOREIGN KEY(selected_option_id) REFERENCES quiz_options(option_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS results (
    result_id INTEGER PRIMARY KEY AUTOINCREMENT,
    attempt_id INTEGER NOT NULL UNIQUE,
    student_id INTEGER NOT NULL,
    quiz_id INTEGER NOT NULL,
    percentage REAL NOT NULL DEFAULT 0,
    published_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(attempt_id) REFERENCES attempts(attempt_id) ON DELETE CASCADE,
    FOREIGN KEY(student_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    FOREIGN KEY(quiz_id) REFERENCES quizzes(quiz_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS audit_logs (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    action TEXT NOT NULL,
    details TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_users_role_active ON users(role, is_active);
CREATE INDEX IF NOT EXISTS idx_quizzes_teacher ON quizzes(teacher_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_quizzes_status ON quizzes(status, starts_at, ends_at);
CREATE INDEX IF NOT EXISTS idx_quiz_questions_order ON quiz_questions(quiz_id, position);
CREATE INDEX IF NOT EXISTS idx_attempts_student ON attempts(student_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_attempts_quiz ON attempts(quiz_id, status);
CREATE INDEX IF NOT EXISTS idx_answers_attempt ON student_answers(attempt_id);
CREATE INDEX IF NOT EXISTS idx_results_student ON results(student_id, published_at DESC);