from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class MetricContract:
    name: str
    tables: tuple[str, ...]
    grain: str
    description: str


METRICS: dict[str, MetricContract] = {
    "learner_course_progress": MetricContract(
        name="learner_course_progress",
        tables=("enrollments", "courses", "chapters", "lessons", "progress"),
        grain="user_course",
        description="Personal progress by course. Must filter by trusted user_id.",
    ),
    "course_completion_rate": MetricContract(
        name="course_completion_rate",
        tables=("courses", "enrollments"),
        grain="course",
        description="Course completion rate from completed enrollments over active enrollments.",
    ),
    "active_enrollments_by_level": MetricContract(
        name="active_enrollments_by_level",
        tables=("enrollments", "courses"),
        grain="course_level",
        description="Active enrollment count grouped by course level.",
    ),
    "active_enrollments_by_course": MetricContract(
        name="active_enrollments_by_course",
        tables=("enrollments", "courses"),
        grain="course",
        description="Active enrollment count grouped by course.",
    ),
    "average_course_rating": MetricContract(
        name="average_course_rating",
        tables=("ratings", "courses"),
        grain="course",
        description="Average course rating using ratings.target_id and ratings.target_type = COURSE.",
    ),
    "learner_submissions": MetricContract(
        name="learner_submissions",
        tables=("submissions", "exercises", "lessons", "chapters", "courses"),
        grain="user_course",
        description="Personal exercise score summary. Must not select answer or submission_data.",
    ),
    "exercise_grade_distribution": MetricContract(
        name="exercise_grade_distribution",
        tables=("submissions", "exercises", "lessons", "chapters", "courses"),
        grain="grade_bucket",
        description="Distribution of exercise grades for instructor/admin review.",
    ),
    "learning_path_completion": MetricContract(
        name="learning_path_completion",
        tables=("learning_paths", "path_progress"),
        grain="path",
        description="Learning path completion by path, personal or platform scope.",
    ),
    "study_time_by_course": MetricContract(
        name="study_time_by_course",
        tables=("analytics", "courses"),
        grain="course",
        description="Study time from analytics events grouped by course.",
    ),
    "revenue_by_course": MetricContract(
        name="revenue_by_course",
        tables=("transactions", "transaction_items", "courses", "payments"),
        grain="course",
        description="Revenue by course from completed transactions and successful payments.",
    ),
    "learner_course_instructors": MetricContract(
        name="learner_course_instructors",
        tables=("enrollments", "courses", "chapters", "lessons", "progress"),
        grain="user_course",
        description="Courses the learner is enrolled in with the owning instructor. Filter by trusted user_id.",
    ),
    "courses_by_instructor": MetricContract(
        name="courses_by_instructor",
        tables=("courses", "users", "profiles", "enrollments"),
        grain="instructor_course",
        description="Published courses owned by a named instructor. Instructor name used only to filter, never projected.",
    ),
    "course_catalog": MetricContract(
        name="course_catalog",
        tables=("courses", "chapters", "lessons", "course_skills", "skills", "ratings"),
        grain="course",
        description="Course availability, level, language, skill coverage, lesson count, and course rating.",
    ),
    "learner_progress": MetricContract(
        name="learner_progress",
        tables=("enrollments", "progress", "lessons", "chapters", "courses"),
        grain="user_course",
        description="Learner enrollment status and lesson completion inside each course.",
    ),
    "learning_path_progress": MetricContract(
        name="learning_path_progress",
        tables=("learning_paths", "learning_path_courses", "path_progress", "courses"),
        grain="user_path",
        description="Learner progress through learning paths and their course nodes.",
    ),
    "exercise_quality": MetricContract(
        name="exercise_quality",
        tables=("exercises", "exercise_test_cases", "submissions", "lessons", "courses"),
        grain="exercise",
        description="Exercise coverage, submission status, grade, and test-case completeness.",
    ),
    "revenue": MetricContract(
        name="revenue",
        tables=("transactions", "transaction_items", "payments", "courses"),
        grain="transaction_course",
        description="Payment and transaction performance by course.",
    ),
    "platform_activity": MetricContract(
        name="platform_activity",
        tables=("analytics", "users", "courses"),
        grain="event",
        description="Raw analytics events such as lesson view, exercise submit, enrollment, or payment.",
    ),
}
