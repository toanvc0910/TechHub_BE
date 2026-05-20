from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class TableContract:
    name: str
    owner: str
    columns: tuple[str, ...]
    pii_columns: tuple[str, ...] = ()
    analytics_safe: bool = True
    ai_readable: bool = True
    ai_writable: bool = False

    @property
    def safe_columns(self) -> tuple[str, ...]:
        blocked = set(self.pii_columns)
        return tuple(column for column in self.columns if column not in blocked)


AUDIT_COLUMNS = ("created", "updated", "created_by", "updated_by", "is_active")


TABLES: dict[str, TableContract] = {
    "users": TableContract(
        name="users",
        owner="user-service",
        columns=(
            "id",
            "email",
            "username",
            "password_hash",
            "avatar",
            "status",
            "login_type",
            *AUDIT_COLUMNS,
        ),
        pii_columns=("email", "username", "password_hash", "avatar"),
    ),
    "profiles": TableContract(
        name="profiles",
        owner="user-service",
        columns=(
            "id",
            "user_id",
            "full_name",
            "avatar_url",
            "bio",
            "location",
            "preferred_language",
            "learning_history",
            *AUDIT_COLUMNS,
        ),
        pii_columns=("full_name", "avatar_url", "bio", "location"),
    ),
    "courses": TableContract(
        name="courses",
        owner="course-service",
        columns=(
            "id",
            "title",
            "description",
            "price",
            "currency",
            "instructor_id",
            "status",
            "level",
            "language",
            "discount_price",
            "promo_end_date",
            "thumbnail",
            "intro_video_file",
            "objectives",
            "requirements",
            *AUDIT_COLUMNS,
        ),
    ),
    "chapters": TableContract(
        name="chapters",
        owner="course-service",
        columns=(
            "id",
            "title",
            "order",
            "course_id",
            "min_completion_threshold",
            "auto_unlock",
            "locked",
            *AUDIT_COLUMNS,
        ),
    ),
    "lessons": TableContract(
        name="lessons",
        owner="course-service",
        columns=(
            "id",
            "title",
            "description",
            "order",
            "chapter_id",
            "content_type",
            "content",
            "mandatory",
            "completion_weight",
            "estimated_duration",
            "is_free",
            "workspace_enabled",
            "workspace_languages",
            "workspace_template",
            "video_url",
            "document_urls",
            *AUDIT_COLUMNS,
        ),
    ),
    "lesson_assets": TableContract(
        name="lesson_assets",
        owner="course-service",
        columns=(
            "id",
            "lesson_id",
            "asset_type",
            "order",
            "title",
            "description",
            "file_id",
            "external_url",
            "metadata",
            *AUDIT_COLUMNS,
        ),
        pii_columns=("external_url",),
    ),
    "exercises": TableContract(
        name="exercises",
        owner="course-service",
        columns=(
            "id",
            "type",
            "question",
            "test_cases",
            "lesson_id",
            "order_index",
            "options",
            *AUDIT_COLUMNS,
        ),
    ),
    "exercise_test_cases": TableContract(
        name="exercise_test_cases",
        owner="course-service",
        columns=(
            "id",
            "exercise_id",
            "order",
            "visibility",
            "input",
            "expected_output",
            "weight",
            "timeout_seconds",
            "sample",
            "metadata",
            *AUDIT_COLUMNS,
        ),
    ),
    "enrollments": TableContract(
        name="enrollments",
        owner="course-service",
        columns=("id", "user_id", "course_id", "status", "enrolled_at", "completed_at", *AUDIT_COLUMNS),
    ),
    "progress": TableContract(
        name="progress",
        owner="course-service",
        columns=("id", "user_id", "lesson_id", "completion", "completed_at", *AUDIT_COLUMNS),
    ),
    "ratings": TableContract(
        name="ratings",
        owner="course-service",
        columns=("id", "user_id", "target_id", "target_type", "score", *AUDIT_COLUMNS),
    ),
    "submissions": TableContract(
        name="submissions",
        owner="course-service",
        columns=(
            "id",
            "user_id",
            "exercise_id",
            "answer",
            "submission_data",
            "grade",
            "graded_at",
            "graded_by",
            "status",
            *AUDIT_COLUMNS,
        ),
        pii_columns=("answer", "submission_data"),
    ),
    "user_codes": TableContract(
        name="user_codes",
        owner="course-service",
        columns=("id", "user_id", "lesson_id", "code", "language", "saved_at", *AUDIT_COLUMNS),
        pii_columns=("code",),
        analytics_safe=False,
    ),
    "learning_paths": TableContract(
        name="learning_paths",
        owner="learning-path-service",
        columns=("id", "title", "description", "layout_edges", *AUDIT_COLUMNS),
    ),
    "learning_path_courses": TableContract(
        name="learning_path_courses",
        owner="learning-path-service",
        columns=("path_id", "course_id", "order", "position_x", "position_y", "is_optional"),
    ),
    "skills": TableContract(
        name="skills",
        owner="course-service",
        columns=("id", "name", "thumbnail", "category", *AUDIT_COLUMNS),
    ),
    "tags": TableContract(
        name="tags",
        owner="course-service",
        columns=("id", "name", *AUDIT_COLUMNS),
    ),
    "course_skills": TableContract(
        name="course_skills",
        owner="course-service",
        columns=("id", "course_id", "skill_id", "assigned_at"),
    ),
    "course_tags": TableContract(
        name="course_tags",
        owner="course-service",
        columns=("course_id", "tag_id", "assigned_at"),
    ),
    "learning_path_skills": TableContract(
        name="learning_path_skills",
        owner="learning-path-service",
        columns=("path_id", "skill_id", "assigned_at"),
    ),
    "path_progress": TableContract(
        name="path_progress",
        owner="learning-path-service",
        columns=("id", "user_id", "path_id", "completion", "milestones", *AUDIT_COLUMNS),
    ),
    "analytics": TableContract(
        name="analytics",
        owner="analytics-service",
        columns=(
            "id",
            "user_id",
            "course_id",
            "study_time",
            "score",
            "event_type",
            "timestamp",
            "device",
            *AUDIT_COLUMNS,
        ),
    ),
    "recommendations": TableContract(
        name="recommendations",
        owner="ai-service",
        columns=(
            "id",
            "user_id",
            "recommended_courses",
            "recommended_paths",
            "generated_at",
            *AUDIT_COLUMNS,
        ),
        ai_writable=True,
    ),
    "transactions": TableContract(
        name="transactions",
        owner="payment-service",
        columns=("id", "user_id", "amount", "status", "refund_reason", "refund_amount", *AUDIT_COLUMNS),
        pii_columns=("refund_reason",),
    ),
    "transaction_items": TableContract(
        name="transaction_items",
        owner="payment-service",
        columns=("id", "transaction_id", "course_id", "price_at_purchase", "quantity", *AUDIT_COLUMNS),
    ),
    "payments": TableContract(
        name="payments",
        owner="payment-service",
        columns=("id", "transaction_id", "method", "status", "gateway_response", *AUDIT_COLUMNS),
        pii_columns=("gateway_response",),
    ),
    "files": TableContract(
        name="files",
        owner="file-service",
        columns=(
            "id",
            "name",
            "original_name",
            "folder_id",
            "user_id",
            "cloudinary_public_id",
            "cloudinary_url",
            "cloudinary_secure_url",
            "storage_provider",
            "bucket_name",
            "object_key",
            "public_url",
            "secure_url",
            "thumbnail_object_key",
            "thumbnail_url",
            "processing_status",
            "processing_error",
            "processed_at",
            "file_type",
            "mime_type",
            "file_size",
            "width",
            "height",
            "duration",
            "alt_text",
            "caption",
            "description",
            "tags",
            "upload_source",
            "reference_id",
            "reference_type",
            *AUDIT_COLUMNS,
        ),
        pii_columns=(
            "original_name",
            "cloudinary_public_id",
            "cloudinary_url",
            "cloudinary_secure_url",
            "object_key",
            "public_url",
            "secure_url",
            "thumbnail_object_key",
            "thumbnail_url",
            "alt_text",
            "caption",
            "description",
        ),
        analytics_safe=False,
    ),
    "file_usage": TableContract(
        name="file_usage",
        owner="file-service",
        columns=("id", "file_id", "used_in_type", "used_in_id", "created"),
        analytics_safe=False,
    ),
    "chat_sessions": TableContract(
        name="chat_sessions",
        owner="ai-service",
        columns=("id", "user_id", "started_at", "ended_at", "context", *AUDIT_COLUMNS),
        pii_columns=("context",),
        analytics_safe=False,
        ai_writable=True,
    ),
    "chat_messages": TableContract(
        name="chat_messages",
        owner="ai-service",
        columns=("id", "session_id", "sender", "content", "metadata", "timestamp", *AUDIT_COLUMNS),
        pii_columns=("content", "metadata"),
        analytics_safe=False,
        ai_writable=True,
    ),
    "ai_generation_tasks": TableContract(
        name="ai_generation_tasks",
        owner="ai-service",
        columns=(
            "id",
            "task_type",
            "status",
            "target_reference",
            "model_used",
            "prompt",
            "request_payload",
            "result_payload",
            "error_message",
            *AUDIT_COLUMNS,
        ),
        pii_columns=("prompt", "request_payload", "result_payload", "error_message"),
        analytics_safe=False,
        ai_writable=True,
    ),
}


ANALYTICS_ALLOWED_TABLES = tuple(
    name for name, contract in TABLES.items() if contract.analytics_safe and contract.ai_readable
)
AI_READABLE_TABLES = tuple(name for name, contract in TABLES.items() if contract.ai_readable)
AI_WRITABLE_TABLES = tuple(name for name, contract in TABLES.items() if contract.ai_writable)
SENSITIVE_COLUMNS = tuple(sorted({column for table in TABLES.values() for column in table.pii_columns}))
ANALYTICS_SENSITIVE_COLUMNS = tuple(
    sorted(
        {
            column
            for table in TABLES.values()
            if table.analytics_safe and table.ai_readable
            for column in table.pii_columns
        }
    )
)
TABLES_WITH_IS_ACTIVE = tuple(name for name, table in TABLES.items() if "is_active" in table.columns)


def quote_column(column: str) -> str:
    return f'"{column}"' if column in {"order"} else column


def render_analytics_schema_context() -> str:
    from app.services.data_contract.joins import ANALYTICS_JOIN_HINTS

    lines = [
        "Tables and safe columns from the TechHub data contract:",
    ]
    for name in ANALYTICS_ALLOWED_TABLES:
        contract = TABLES[name]
        safe_columns = ", ".join(quote_column(column) for column in contract.safe_columns)
        lines.append(f"- {name}({safe_columns})")
    lines.extend(
        [
            "",
            "Important join rules:",
            *[f"- {hint}" for hint in ANALYTICS_JOIN_HINTS],
            "",
            "Only generate read-only PostgreSQL SQL. Use SELECT or WITH only.",
            "Prefer aggregations useful for analytics dashboards.",
            "When grouping, alias the category column as label and the numeric aggregation as value.",
            "Do not select raw personal columns unless runtime policy explicitly allows PII.",
        ]
    )
    return "\n".join(lines)
