from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class JoinContract:
    left_table: str
    left_column: str
    right_table: str
    right_column: str
    purpose: str


JOINS: tuple[JoinContract, ...] = (
    JoinContract("profiles", "user_id", "users", "id", "User profile belongs to a user."),
    JoinContract("courses", "instructor_id", "users", "id", "Course instructor user."),
    JoinContract("chapters", "course_id", "courses", "id", "Course contains chapters."),
    JoinContract("lessons", "chapter_id", "chapters", "id", "Chapter contains lessons."),
    JoinContract("lesson_assets", "lesson_id", "lessons", "id", "Lesson owns attached assets."),
    JoinContract("exercises", "lesson_id", "lessons", "id", "Exercise belongs to a lesson."),
    JoinContract("exercise_test_cases", "exercise_id", "exercises", "id", "Coding test case belongs to an exercise."),
    JoinContract("enrollments", "user_id", "users", "id", "Learner enrollment user."),
    JoinContract("enrollments", "course_id", "courses", "id", "Enrollment points to a course."),
    JoinContract("progress", "user_id", "users", "id", "Lesson progress user."),
    JoinContract("progress", "lesson_id", "lessons", "id", "Progress is measured per lesson."),
    JoinContract("ratings", "user_id", "users", "id", "Rating author."),
    JoinContract("ratings", "target_id", "courses", "id", "Course rating when target_type = 'COURSE'."),
    JoinContract("submissions", "user_id", "users", "id", "Submission author."),
    JoinContract("submissions", "exercise_id", "exercises", "id", "Submission target exercise."),
    JoinContract("learning_path_courses", "path_id", "learning_paths", "id", "Learning path contains courses."),
    JoinContract("learning_path_courses", "course_id", "courses", "id", "Learning path course node."),
    JoinContract("path_progress", "user_id", "users", "id", "Learning path progress user."),
    JoinContract("path_progress", "path_id", "learning_paths", "id", "Progress belongs to a learning path."),
    JoinContract("course_skills", "course_id", "courses", "id", "Course skill mapping."),
    JoinContract("course_skills", "skill_id", "skills", "id", "Skill assigned to a course."),
    JoinContract("course_tags", "course_id", "courses", "id", "Course tag mapping."),
    JoinContract("course_tags", "tag_id", "tags", "id", "Tag assigned to a course."),
    JoinContract("learning_path_skills", "path_id", "learning_paths", "id", "Learning path skill mapping."),
    JoinContract("learning_path_skills", "skill_id", "skills", "id", "Skill assigned to a learning path."),
    JoinContract("analytics", "user_id", "users", "id", "Analytics event user."),
    JoinContract("analytics", "course_id", "courses", "id", "Analytics event course."),
    JoinContract("recommendations", "user_id", "users", "id", "Stored AI recommendation user."),
    JoinContract("transactions", "user_id", "users", "id", "Transaction buyer."),
    JoinContract("transaction_items", "transaction_id", "transactions", "id", "Transaction contains purchased courses."),
    JoinContract("transaction_items", "course_id", "courses", "id", "Purchased course."),
    JoinContract("payments", "transaction_id", "transactions", "id", "Payment belongs to transaction."),
    JoinContract("files", "user_id", "users", "id", "Uploaded file owner."),
    JoinContract("file_usage", "file_id", "files", "id", "File usage mapping."),
    JoinContract("chat_messages", "session_id", "chat_sessions", "id", "Chat messages belong to a chat session."),
)


ANALYTICS_JOIN_HINTS = (
    "ratings does not have course_id or rating columns; use ratings.target_id = courses.id and ratings.score where ratings.target_type = 'COURSE'.",
    "courses connect to lessons through courses.id -> chapters.course_id -> lessons.chapter_id.",
    "course skill names require courses -> course_skills -> skills.",
    "learning paths connect to courses through learning_path_courses.",
    "payment revenue uses transactions -> transaction_items -> courses and payments through payments.transaction_id.",
    "There is no course_prerequisites table in the current TechHub schema.",
)


MISSING_RELATIONS = {
    "course_prerequisites": (
        "No table exists in techhub.sql. Course dependency logic must use course requirements/objectives "
        "until a real prerequisite relation is added."
    ),
    "embedding_tables": (
        "Vector collections are external Qdrant collections, not PostgreSQL tables. Their payload contract is handled "
        "in the vector contract workstream."
    ),
}
