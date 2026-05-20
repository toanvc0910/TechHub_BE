from __future__ import annotations

from typing import Any

from app.services.analytics.metric_registry import MetricDefinition


def build_metric_sql(
    definition: MetricDefinition,
    *,
    scope: str,
    user_role: str,
    time_range: str,
) -> str:
    time_filter = _time_filter(time_range)
    owner_filter = _owner_filter(definition, user_role)

    if definition.key == "learner_course_progress":
        return f"""
            SELECT
                c.title AS label,
                ROUND(COALESCE(AVG(COALESCE(p.completion, 0.0)) * 100, 0.0)::numeric, 2) AS value,
                COUNT(DISTINCT CASE WHEN COALESCE(p.completion, 0.0) >= 1 THEN l.id END)::int AS completed_lessons,
                COUNT(DISTINCT l.id)::int AS total_lessons
            FROM enrollments e
            JOIN courses c
                ON c.id = e.course_id
               AND c.is_active = 'Y'
               AND c.status = 'PUBLISHED'
            LEFT JOIN chapters ch
                ON ch.course_id = c.id
               AND ch.is_active = 'Y'
            LEFT JOIN lessons l
                ON l.chapter_id = ch.id
               AND l.is_active = 'Y'
            LEFT JOIN progress p
                ON p.lesson_id = l.id
               AND p.user_id = e.user_id
               AND p.is_active = 'Y'
            WHERE e.is_active = 'Y'
              AND e.user_id = :user_id
              {time_filter('COALESCE(p.updated, e.updated, e.created)')}
            GROUP BY c.id, c.title
            ORDER BY MAX(e.updated) DESC NULLS LAST, value DESC, c.title ASC
            LIMIT 20
        """

    if definition.key == "course_completion_rate":
        return f"""
            SELECT
                c.title AS label,
                ROUND(
                    CASE WHEN COUNT(e.id) = 0 THEN 0
                    ELSE (COUNT(CASE WHEN e.status = 'COMPLETED' THEN 1 END)::numeric / COUNT(e.id)::numeric) * 100
                    END,
                    2
                ) AS value,
                COUNT(e.id)::int AS enrollment_count,
                COUNT(CASE WHEN e.status = 'COMPLETED' THEN 1 END)::int AS completed_count
            FROM courses c
            LEFT JOIN enrollments e
                ON e.course_id = c.id
               AND e.is_active = 'Y'
               {time_filter('COALESCE(e.completed_at, e.updated, e.created)')}
            WHERE c.is_active = 'Y'
              AND c.status = 'PUBLISHED'
              {owner_filter}
            GROUP BY c.id, c.title
            ORDER BY value DESC, enrollment_count DESC, c.title ASC
            LIMIT 20
        """

    if definition.key == "active_enrollments_by_level":
        return f"""
            SELECT
                COALESCE(c.level::text, 'UNKNOWN') AS label,
                COUNT(DISTINCT e.id)::int AS value
            FROM enrollments e
            JOIN courses c
                ON c.id = e.course_id
               AND c.is_active = 'Y'
               AND c.status = 'PUBLISHED'
            WHERE e.is_active = 'Y'
              AND e.status IN ('ENROLLED', 'IN_PROGRESS')
              {owner_filter}
              {time_filter('COALESCE(e.updated, e.created)')}
            GROUP BY COALESCE(c.level::text, 'UNKNOWN')
            ORDER BY value DESC, label ASC
            LIMIT 20
        """

    if definition.key == "active_enrollments_by_course":
        return f"""
            SELECT
                c.title AS label,
                COUNT(DISTINCT e.id)::int AS value
            FROM enrollments e
            JOIN courses c
                ON c.id = e.course_id
               AND c.is_active = 'Y'
               AND c.status = 'PUBLISHED'
            WHERE e.is_active = 'Y'
              AND e.status IN ('ENROLLED', 'IN_PROGRESS')
              {owner_filter}
              {time_filter('COALESCE(e.updated, e.created)')}
            GROUP BY c.id, c.title
            ORDER BY value DESC, c.title ASC
            LIMIT 20
        """

    if definition.key == "average_course_rating":
        return f"""
            SELECT
                c.title AS label,
                ROUND(COALESCE(AVG(r.score), 0)::numeric, 2) AS value,
                COUNT(r.id)::int AS rating_count
            FROM courses c
            LEFT JOIN ratings r
                ON r.target_id = c.id
               AND r.target_type = 'COURSE'
               AND r.is_active = 'Y'
               {time_filter('r.created')}
            WHERE c.is_active = 'Y'
              AND c.status = 'PUBLISHED'
              {owner_filter}
            GROUP BY c.id, c.title
            ORDER BY value DESC, rating_count DESC, c.title ASC
            LIMIT 20
        """

    if definition.key == "learner_submissions":
        return f"""
            SELECT
                c.title AS label,
                ROUND(COALESCE(AVG(s.grade), 0)::numeric, 2) AS value,
                COUNT(s.id)::int AS submission_count,
                COUNT(CASE WHEN s.status = 'PASSED' THEN 1 END)::int AS passed_count
            FROM submissions s
            JOIN exercises ex
                ON ex.id = s.exercise_id
               AND ex.is_active = 'Y'
            JOIN lessons l
                ON l.id = ex.lesson_id
               AND l.is_active = 'Y'
            JOIN chapters ch
                ON ch.id = l.chapter_id
               AND ch.is_active = 'Y'
            JOIN courses c
                ON c.id = ch.course_id
               AND c.is_active = 'Y'
            WHERE s.is_active = 'Y'
              AND s.user_id = :user_id
              {time_filter('COALESCE(s.graded_at, s.updated, s.created)')}
            GROUP BY c.id, c.title
            ORDER BY MAX(s.updated) DESC NULLS LAST, value DESC, c.title ASC
            LIMIT 20
        """

    if definition.key == "exercise_grade_distribution":
        return f"""
            SELECT
                CASE
                    WHEN s.grade IS NULL THEN 'Chua cham'
                    WHEN s.grade < 5 THEN 'Duoi 5'
                    WHEN s.grade < 7 THEN '5 den duoi 7'
                    WHEN s.grade < 8.5 THEN '7 den duoi 8.5'
                    ELSE 'Tu 8.5 tro len'
                END AS label,
                COUNT(s.id)::int AS value
            FROM submissions s
            JOIN exercises ex
                ON ex.id = s.exercise_id
               AND ex.is_active = 'Y'
            JOIN lessons l
                ON l.id = ex.lesson_id
               AND l.is_active = 'Y'
            JOIN chapters ch
                ON ch.id = l.chapter_id
               AND ch.is_active = 'Y'
            JOIN courses c
                ON c.id = ch.course_id
               AND c.is_active = 'Y'
            WHERE s.is_active = 'Y'
              {owner_filter}
              {time_filter('COALESCE(s.graded_at, s.updated, s.created)')}
            GROUP BY label
            ORDER BY value DESC, label ASC
            LIMIT 20
        """

    if definition.key == "learning_path_completion":
        personal_filter = "AND pp.user_id = :user_id" if scope == "personal" else ""
        return f"""
            SELECT
                lp.title AS label,
                ROUND(
                    COALESCE(AVG(CASE WHEN pp.completion <= 1 THEN pp.completion * 100 ELSE pp.completion END), 0)::numeric,
                    2
                ) AS value,
                COUNT(pp.id)::int AS learner_count
            FROM learning_paths lp
            LEFT JOIN path_progress pp
                ON pp.path_id = lp.id
               AND pp.is_active = 'Y'
               {personal_filter}
               {time_filter('COALESCE(pp.updated, pp.created)')}
            WHERE lp.is_active = 'Y'
            GROUP BY lp.id, lp.title
            ORDER BY value DESC, learner_count DESC, lp.title ASC
            LIMIT 20
        """

    if definition.key == "study_time_by_course":
        personal_filter = "AND a.user_id = :user_id" if scope == "personal" else ""
        return f"""
            SELECT
                c.title AS label,
                COALESCE(SUM(a.study_time), 0)::bigint AS value,
                COUNT(a.id)::int AS event_count
            FROM analytics a
            JOIN courses c
                ON c.id = a.course_id
               AND c.is_active = 'Y'
            WHERE a.is_active = 'Y'
              AND a.study_time IS NOT NULL
              {personal_filter}
              {owner_filter}
              {time_filter('a.timestamp')}
            GROUP BY c.id, c.title
            ORDER BY value DESC, c.title ASC
            LIMIT 20
        """

    if definition.key == "revenue_by_course":
        return f"""
            SELECT
                c.title AS label,
                ROUND(COALESCE(SUM(ti.price_at_purchase * COALESCE(ti.quantity, 1)), 0)::numeric, 2) AS value,
                COUNT(DISTINCT t.id)::int AS order_count,
                COALESCE(SUM(ti.quantity), 0)::int AS sold_count
            FROM transaction_items ti
            JOIN transactions t
                ON t.id = ti.transaction_id
               AND t.is_active = 'Y'
               AND t.status = 'COMPLETED'
            JOIN courses c
                ON c.id = ti.course_id
               AND c.is_active = 'Y'
            WHERE ti.is_active = 'Y'
              AND EXISTS (
                    SELECT 1
                    FROM payments p
                    WHERE p.transaction_id = t.id
                      AND p.is_active = 'Y'
                      AND p.status = 'SUCCESS'
              )
              {owner_filter}
              {time_filter('t.created')}
            GROUP BY c.id, c.title
            ORDER BY value DESC, order_count DESC, c.title ASC
            LIMIT 20
        """

    raise ValueError(f"No SQL template registered for metric: {definition.key}")


def build_params(definition: MetricDefinition, *, scope: str, user_role: str, user_id: str | None) -> dict[str, Any]:
    params: dict[str, Any] = {}
    needs_user = "user_id" in definition.required_params
    needs_user = needs_user or scope == "personal"
    needs_user = needs_user or user_role in definition.owner_filtered_for_roles
    if needs_user:
        if not user_id:
            raise ValueError(f"Metric '{definition.key}' requires trusted user_id.")
        params["user_id"] = user_id
    return params


def _owner_filter(definition: MetricDefinition, user_role: str) -> str:
    if user_role in definition.owner_filtered_for_roles:
        return "AND c.instructor_id = :user_id"
    return ""


def _time_filter(time_range: str):
    mapping = {
        "today": "AND {column} >= date_trunc('day', now())",
        "this_month": "AND {column} >= date_trunc('month', now())",
        "last_30_days": "AND {column} >= now() - interval '30 days'",
    }

    def render(column: str) -> str:
        template = mapping.get(str(time_range or "").lower())
        return template.format(column=column) if template else ""

    return render
