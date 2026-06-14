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

    if definition.key == "learner_course_instructors":
        # List the courses the learner is enrolled in together with the owning
        # instructor. The instructor's display name is PII, so we only project
        # the non-sensitive instructor_id here; the analytics service resolves
        # it to a public display name in a separate controlled lookup.
        return f"""
            SELECT
                c.title AS label,
                c.instructor_id AS instructor_id,
                ROUND(COALESCE(AVG(COALESCE(p.completion, 0.0)) * 100, 0.0)::numeric, 2) AS value
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
            GROUP BY c.id, c.title, c.instructor_id
            ORDER BY MAX(e.updated) DESC NULLS LAST, c.title ASC
            LIMIT 50
        """

    if definition.key == "courses_by_instructor":
        # Published courses owned by a named instructor. The instructor name is
        # only used to FILTER (in WHERE / JOIN), never projected, so the PII
        # guard stays satisfied. The instructor display name is resolved later
        # by the analytics service for output.
        return f"""
            SELECT
                c.title AS label,
                c.instructor_id AS instructor_id,
                COUNT(DISTINCT e.id)::int AS value
            FROM courses c
            JOIN users u
                ON u.id = c.instructor_id
               AND u.is_active = 'Y'
            LEFT JOIN profiles pr
                ON pr.user_id = u.id
               AND pr.is_active = 'Y'
            LEFT JOIN enrollments e
                ON e.course_id = c.id
               AND e.is_active = 'Y'
            WHERE c.is_active = 'Y'
              AND c.status = 'PUBLISHED'
              AND (
                    lower(u.username) LIKE '%' || lower(:instructor_name) || '%'
                 OR lower(pr.full_name) LIKE '%' || lower(:instructor_name) || '%'
              )
              {time_filter('COALESCE(c.updated, c.created)')}
            GROUP BY c.id, c.title, c.instructor_id
            ORDER BY value DESC, c.title ASC
            LIMIT 50
        """

    if definition.key == "course_catalog":
        # Public course catalog. ":topic" is an optional free-text filter that
        # matches the course title, description, or an attached skill name. When
        # empty it is a no-op so the same template lists the whole catalog.
        return f"""
            SELECT
                c.title AS label,
                COUNT(DISTINCT e.id)::int AS value,
                c.price AS price,
                c.level::text AS level,
                c.id AS course_id,
                c.instructor_id AS instructor_id
            FROM courses c
            LEFT JOIN enrollments e
                ON e.course_id = c.id
               AND e.is_active = 'Y'
            LEFT JOIN course_skills cs
                ON cs.course_id = c.id
            LEFT JOIN skills sk
                ON sk.id = cs.skill_id
               AND sk.is_active = 'Y'
            WHERE c.is_active = 'Y'
              AND c.status = 'PUBLISHED'
              AND (
                    :topic = ''
                 OR lower(c.title) LIKE '%' || lower(:topic) || '%'
                 OR lower(COALESCE(c.description, '')) LIKE '%' || lower(:topic) || '%'
                 OR lower(COALESCE(sk.name, '')) LIKE '%' || lower(:topic) || '%'
              )
              {time_filter('COALESCE(c.updated, c.created)')}
            GROUP BY c.id, c.title, c.price, c.level, c.instructor_id
            ORDER BY value DESC, c.title ASC
            LIMIT 50
        """

    if definition.key == "recommended_next_courses":
        # Personalized "what to learn next": published courses the learner has
        # NOT enrolled in, ranked by how many skills they share with what the
        # learner is already taking (so a FE learner keeps getting FE courses,
        # etc.), then by overall popularity for newcomers with no skill overlap.
        return """
            WITH my_courses AS (
                SELECT course_id
                FROM enrollments
                WHERE is_active = 'Y' AND user_id = :user_id
            ),
            my_skills AS (
                SELECT DISTINCT cs.skill_id
                FROM course_skills cs
                JOIN my_courses mc ON mc.course_id = cs.course_id
            )
            SELECT
                c.title AS label,
                COUNT(DISTINCT cs.skill_id)
                    FILTER (WHERE cs.skill_id IN (SELECT skill_id FROM my_skills))::int AS value,
                c.price AS price,
                c.level::text AS level,
                c.id AS course_id,
                c.instructor_id AS instructor_id,
                COUNT(DISTINCT e.id)::int AS enrollment_count
            FROM courses c
            LEFT JOIN course_skills cs
                ON cs.course_id = c.id
            LEFT JOIN enrollments e
                ON e.course_id = c.id
               AND e.is_active = 'Y'
            WHERE c.is_active = 'Y'
              AND c.status = 'PUBLISHED'
              AND c.id NOT IN (SELECT course_id FROM my_courses)
            GROUP BY c.id, c.title, c.price, c.level, c.instructor_id
            ORDER BY value DESC, enrollment_count DESC, c.title ASC
            LIMIT 20
        """

    if definition.key == "course_pricing":
        return f"""
            SELECT
                c.title AS label,
                c.price AS value,
                c.discount_price AS discount_price,
                c.level::text AS level
            FROM courses c
            WHERE c.is_active = 'Y'
              AND c.status = 'PUBLISHED'
              {time_filter('COALESCE(c.updated, c.created)')}
            ORDER BY c.price DESC, c.title ASC
            LIMIT 50
        """

    if definition.key == "course_structure":
        # Lessons inside a named course, ordered by chapter then lesson order.
        # The course name is only used to FILTER, never projected.
        return """
            SELECT
                l.title AS label,
                COALESCE(l.estimated_duration, 0)::int AS value,
                ch.title AS chapter,
                l.content_type::text AS content_type
            FROM courses c
            JOIN chapters ch
                ON ch.course_id = c.id
               AND ch.is_active = 'Y'
            JOIN lessons l
                ON l.chapter_id = ch.id
               AND l.is_active = 'Y'
            WHERE c.is_active = 'Y'
              AND c.status = 'PUBLISHED'
              AND lower(c.title) LIKE '%' || lower(:course_name) || '%'
            ORDER BY ch."order" ASC, l."order" ASC
            LIMIT 100
        """

    if definition.key == "learning_path_catalog":
        # ":topic" optionally narrows the list to paths whose title matches the
        # requested subject (e.g. "gợi ý lộ trình devops"); no-op when empty so
        # the same template also lists the whole catalog.
        return """
            SELECT
                lp.title AS label,
                COUNT(DISTINCT lpc.course_id)::int AS value
            FROM learning_paths lp
            LEFT JOIN learning_path_courses lpc
                ON lpc.path_id = lp.id
            WHERE lp.is_active = 'Y'
              AND (
                    :topic = ''
                 OR lower(lp.title) LIKE '%' || lower(:topic) || '%'
              )
            GROUP BY lp.id, lp.title
            ORDER BY value DESC, lp.title ASC
            LIMIT 50
        """

    if definition.key == "learning_path_courses":
        # Courses inside a named learning path, in their defined order. The path
        # name is only used to FILTER, never projected.
        return """
            SELECT
                c.title AS label,
                lpc."order" AS value,
                lp.title AS path
            FROM learning_paths lp
            JOIN learning_path_courses lpc
                ON lpc.path_id = lp.id
            JOIN courses c
                ON c.id = lpc.course_id
               AND c.is_active = 'Y'
            WHERE lp.is_active = 'Y'
              AND lower(lp.title) LIKE '%' || lower(:path_name) || '%'
            ORDER BY lpc."order" ASC, c.title ASC
            LIMIT 100
        """

    if definition.key == "blog_catalog":
        # Published blogs, newest first. ":topic" optionally filters by title,
        # content, or tag array. Value is the count of related courses. The
        # bounded `content` column lets the assistant answer "what's in blog X?"
        # with the actual body text instead of guessing from the numeric value.
        return """
            SELECT
                b.title AS label,
                COALESCE(array_length(b.related_course_ids, 1), 0)::int AS value,
                to_char(b.created, 'YYYY-MM-DD') AS created_at,
                LEFT(COALESCE(b.content, ''), 8000) AS content
            FROM blogs b
            WHERE b.is_active = 'Y'
              AND b.status = 'PUBLISHED'
              AND (
                    :topic = ''
                 OR lower(b.title) LIKE '%' || lower(:topic) || '%'
                 OR lower(COALESCE(b.content, '')) LIKE '%' || lower(:topic) || '%'
                 OR lower(COALESCE(array_to_string(b.tags, ' '), '')) LIKE '%' || lower(:topic) || '%'
              )
            ORDER BY b.created DESC
            LIMIT 50
        """

    raise ValueError(f"No SQL template registered for metric: {definition.key}")


# Metrics that accept an optional free-text ":topic" filter (no-op when empty).
_OPTIONAL_TOPIC_METRICS = frozenset({"course_catalog", "blog_catalog", "learning_path_catalog"})


def build_params(
    definition: MetricDefinition,
    *,
    scope: str,
    user_role: str,
    user_id: str | None,
    entities: dict[str, Any] | None = None,
) -> dict[str, Any]:
    ents = entities or {}
    params: dict[str, Any] = {}
    needs_user = "user_id" in definition.required_params
    needs_user = needs_user or scope == "personal"
    needs_user = needs_user or user_role in definition.owner_filtered_for_roles
    if needs_user:
        if not user_id:
            raise ValueError(f"Metric '{definition.key}' requires trusted user_id.")
        params["user_id"] = user_id
    # Every required param other than user_id is pulled from the extracted
    # entities (e.g. instructor_name, course_name, path_name).
    for param in definition.required_params:
        if param == "user_id":
            continue
        value = str(ents.get(param) or "").strip()
        if not value:
            raise ValueError(f"Metric '{definition.key}' requires '{param}'.")
        params[param] = value
    if definition.key in _OPTIONAL_TOPIC_METRICS:
        params["topic"] = str(ents.get("topic") or "").strip()
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
