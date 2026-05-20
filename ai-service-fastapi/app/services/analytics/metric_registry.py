from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class MetricDefinition:
    key: str
    title: str
    description: str
    allowed_scopes: tuple[str, ...]
    required_roles: tuple[str, ...]
    tables: tuple[str, ...]
    required_params: tuple[str, ...]
    default_chart: str
    grain: str
    owner_filtered_for_roles: tuple[str, ...] = ()


ANY_AUTHENTICATED = ("LEARNER", "INSTRUCTOR", "STAFF", "ADMIN", "SUPER_ADMIN")
CONTENT_ROLES = ("INSTRUCTOR", "STAFF", "ADMIN", "SUPER_ADMIN")
ADMIN_ROLES = ("STAFF", "ADMIN", "SUPER_ADMIN")


METRIC_REGISTRY: dict[str, MetricDefinition] = {
    "learner_course_progress": MetricDefinition(
        key="learner_course_progress",
        title="Tien do hoc tap cua ban theo khoa hoc",
        description="Personal course progress from enrollments, lessons, and progress rows.",
        allowed_scopes=("personal",),
        required_roles=ANY_AUTHENTICATED,
        tables=("enrollments", "courses", "chapters", "lessons", "progress"),
        required_params=("user_id",),
        default_chart="bar",
        grain="user_course",
    ),
    "course_completion_rate": MetricDefinition(
        key="course_completion_rate",
        title="Ty le hoan thanh theo khoa hoc",
        description="Course completion rate from enrollment status.",
        allowed_scopes=("platform",),
        required_roles=CONTENT_ROLES,
        tables=("courses", "enrollments"),
        required_params=(),
        default_chart="bar",
        grain="course",
        owner_filtered_for_roles=("INSTRUCTOR",),
    ),
    "active_enrollments_by_level": MetricDefinition(
        key="active_enrollments_by_level",
        title="Hoc vien dang hoc theo cap do khoa hoc",
        description="Active enrollments grouped by course level.",
        allowed_scopes=("platform",),
        required_roles=CONTENT_ROLES,
        tables=("enrollments", "courses"),
        required_params=(),
        default_chart="bar",
        grain="course_level",
        owner_filtered_for_roles=("INSTRUCTOR",),
    ),
    "active_enrollments_by_course": MetricDefinition(
        key="active_enrollments_by_course",
        title="Khoa hoc co nhieu hoc vien dang hoc",
        description="Active enrollments grouped by course.",
        allowed_scopes=("platform",),
        required_roles=CONTENT_ROLES,
        tables=("enrollments", "courses"),
        required_params=(),
        default_chart="bar",
        grain="course",
        owner_filtered_for_roles=("INSTRUCTOR",),
    ),
    "average_course_rating": MetricDefinition(
        key="average_course_rating",
        title="Diem danh gia trung binh tung khoa",
        description="Average course rating using ratings.target_id and ratings.target_type.",
        allowed_scopes=("platform",),
        required_roles=CONTENT_ROLES,
        tables=("ratings", "courses"),
        required_params=(),
        default_chart="bar",
        grain="course",
        owner_filtered_for_roles=("INSTRUCTOR",),
    ),
    "learner_submissions": MetricDefinition(
        key="learner_submissions",
        title="Ket qua lam bai tap cua ban",
        description="Personal exercise scores from submissions joined to exercises, lessons, chapters, and courses.",
        allowed_scopes=("personal",),
        required_roles=ANY_AUTHENTICATED,
        tables=("submissions", "exercises", "lessons", "chapters", "courses"),
        required_params=("user_id",),
        default_chart="bar",
        grain="user_course_lesson",
    ),
    "exercise_grade_distribution": MetricDefinition(
        key="exercise_grade_distribution",
        title="Phan bo diem bai tap",
        description="Grade buckets from submissions for instructor/admin review.",
        allowed_scopes=("platform",),
        required_roles=CONTENT_ROLES,
        tables=("submissions", "exercises", "lessons", "chapters", "courses"),
        required_params=(),
        default_chart="bar",
        grain="grade_bucket",
        owner_filtered_for_roles=("INSTRUCTOR",),
    ),
    "learning_path_completion": MetricDefinition(
        key="learning_path_completion",
        title="Ty le hoan thanh learning path",
        description="Learning path completion from path_progress.",
        allowed_scopes=("personal", "platform"),
        required_roles=ANY_AUTHENTICATED,
        tables=("learning_paths", "path_progress"),
        required_params=(),
        default_chart="bar",
        grain="path",
    ),
    "study_time_by_course": MetricDefinition(
        key="study_time_by_course",
        title="Thoi gian hoc theo khoa",
        description="Study time from analytics events grouped by course.",
        allowed_scopes=("personal", "platform"),
        required_roles=ANY_AUTHENTICATED,
        tables=("analytics", "courses"),
        required_params=(),
        default_chart="bar",
        grain="course",
        owner_filtered_for_roles=("INSTRUCTOR",),
    ),
    "revenue_by_course": MetricDefinition(
        key="revenue_by_course",
        title="Doanh thu theo khoa",
        description="Revenue from completed transactions and purchased course line items.",
        allowed_scopes=("platform",),
        required_roles=("INSTRUCTOR", "STAFF", "ADMIN", "SUPER_ADMIN"),
        tables=("transactions", "transaction_items", "courses", "payments"),
        required_params=(),
        default_chart="bar",
        grain="course",
        owner_filtered_for_roles=("INSTRUCTOR",),
    ),
}


def get_metric(key: str | None) -> MetricDefinition | None:
    if not key:
        return None
    return METRIC_REGISTRY.get(key)


def list_metrics() -> tuple[MetricDefinition, ...]:
    return tuple(METRIC_REGISTRY.values())
