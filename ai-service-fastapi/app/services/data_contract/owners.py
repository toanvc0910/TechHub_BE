from __future__ import annotations

from app.services.data_contract.tables import AI_WRITABLE_TABLES, TABLES


DATA_OWNERS = {
    "user-service": ("users", "profiles"),
    "course-service": (
        "courses",
        "chapters",
        "lessons",
        "lesson_assets",
        "exercises",
        "exercise_test_cases",
        "enrollments",
        "progress",
        "ratings",
        "submissions",
        "user_codes",
        "skills",
        "tags",
        "course_skills",
        "course_tags",
    ),
    "learning-path-service": (
        "learning_paths",
        "learning_path_courses",
        "learning_path_skills",
        "path_progress",
    ),
    "blog-service": ("blogs",),
    "analytics-service": ("analytics",),
    "payment-service": ("transactions", "transaction_items", "payments"),
    "file-service": ("files", "file_usage"),
    "ai-service": ("recommendations", "chat_sessions", "chat_messages", "ai_generation_tasks"),
}


OWNER_BY_TABLE = {name: contract.owner for name, contract in TABLES.items()}
AI_DIRECT_WRITE_TABLES = AI_WRITABLE_TABLES
