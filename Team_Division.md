# Three-Member Work Division

## Member 1 — Authentication and Student Interface
- Student/Teacher registration and login
- Password hashing and role-based dashboard
- Student quiz discovery, access-code entry and result history
- UI validation and navigation testing

## Member 2 — Database and Teacher Module
- SQLite schema and JDBC integration
- Subject/category and question-bank management
- Add, edit and delete MCQs
- Quiz creation, publishing and closing
- Teacher analytics and report validation

## Member 3 — Examination Engine and Integration
- Timed quiz interface and question navigation
- Answer retention and automatic submission
- Score calculation and result persistence
- Leaderboard, integration testing and final demonstration

## Integration checklist
1. Run `mvn clean compile`.
2. Register a Teacher account and add at least 10 questions in one category.
3. Publish a quiz and note its access code.
4. Register a Student account, join the quiz and complete it.
5. Verify student result history and teacher analytics.
6. Test timer expiry, empty answers, invalid access codes and duplicate emails.
