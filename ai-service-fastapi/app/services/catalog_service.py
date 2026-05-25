from __future__ import annotations

import json
from collections import defaultdict
from collections.abc import Iterable
from typing import Any

from sqlalchemy import text

from app.db.session import get_db_session


def _normalize_uuidish(value: Any) -> str | None:
    if value is None:
        return None
    return str(value)


def _normalize_jsonish(value: Any, default: Any) -> Any:
    if value is None:
        return default
    if isinstance(value, (list, dict)):
        return value
    if isinstance(value, str):
        try:
            parsed = json.loads(value)
            if isinstance(parsed, type(default)):
                return parsed
        except json.JSONDecodeError:
            pass
    return default


class CatalogService:
    async def fetch_published_courses(self, limit: int | None = None) -> list[dict[str, Any]]:
        sql = """
            SELECT
                c.id,
                c.title,
                c.description,
                c.objectives,
                c.requirements,
                c.level,
                c.language,
                c.thumbnail,
                c.instructor_id,
                c.status,
                c.created,
                c.updated,
                COALESCE(
                    json_agg(DISTINCT t.name) FILTER (WHERE t.name IS NOT NULL),
                    '[]'::json
                ) AS tags,
                COALESCE(
                    json_agg(DISTINCT s.name) FILTER (WHERE s.name IS NOT NULL),
                    '[]'::json
                ) AS skills,
                COUNT(DISTINCT e.id) FILTER (WHERE e.is_active = 'Y') AS enrollment_count
            FROM courses c
            LEFT JOIN course_tags ct ON c.id = ct.course_id
            LEFT JOIN tags t ON ct.tag_id = t.id AND t.is_active = 'Y'
            LEFT JOIN course_skills cs ON c.id = cs.course_id
            LEFT JOIN skills s ON cs.skill_id = s.id
            LEFT JOIN enrollments e ON e.course_id = c.id
            WHERE c.is_active = 'Y'
              AND c.status = 'PUBLISHED'
            GROUP BY c.id
            ORDER BY c.created DESC
        """
        if limit:
            sql += "\nLIMIT :limit"

        async with get_db_session() as session:
            result = await session.execute(text(sql), {"limit": limit} if limit else {})
            return [self._normalize_course_row(dict(row)) for row in result.mappings().all()]

    async def fetch_courses_by_ids(self, course_ids: Iterable[str]) -> list[dict[str, Any]]:
        ids = [str(course_id) for course_id in course_ids if course_id]
        if not ids:
            return []

        courses = await self.fetch_published_courses(limit=None)
        by_id = {course["id"]: course for course in courses}
        return [by_id[course_id] for course_id in ids if course_id in by_id]

    async def fetch_lessons(self) -> list[dict[str, Any]]:
        sql = """
            SELECT
                l.id,
                l.title,
                l.description,
                l.content,
                l.video_url,
                l.chapter_id,
                l.content_type,
                l.document_urls,
                l.workspace_enabled,
                l.workspace_languages,
                l.workspace_template,
                l.estimated_duration,
                l.is_free,
                l.mandatory,
                c.course_id,
                crs.title AS course_title,
                crs.level AS course_level,
                crs.language AS course_language
            FROM lessons l
            JOIN chapters c ON l.chapter_id = c.id
            JOIN courses crs ON crs.id = c.course_id
            WHERE l.is_active = 'Y'
              AND crs.is_active = 'Y'
            ORDER BY crs.created DESC, c."order" ASC NULLS LAST, l."order" ASC NULLS LAST
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql))
            return [self._normalize_lesson_row(dict(row)) for row in result.mappings().all()]

    async def fetch_lesson_by_id(self, lesson_id: str) -> dict[str, Any] | None:
        sql = """
            SELECT
                l.id,
                l.title,
                l.description,
                l.content,
                l.video_url,
                l.chapter_id,
                l.content_type,
                l.document_urls,
                l.workspace_enabled,
                l.workspace_languages,
                l.workspace_template,
                l.estimated_duration,
                l.is_free,
                l.mandatory,
                c.course_id,
                crs.title AS course_title,
                crs.level AS course_level,
                crs.language AS course_language
            FROM lessons l
            JOIN chapters c ON l.chapter_id = c.id
            JOIN courses crs ON crs.id = c.course_id
            WHERE l.id = :lesson_id
              AND l.is_active = 'Y'
              AND crs.is_active = 'Y'
            LIMIT 1
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql), {"lesson_id": lesson_id})
            row = result.mappings().first()
            return self._normalize_lesson_row(dict(row)) if row else None

    async def fetch_user_profile(self, user_id: str) -> dict[str, Any] | None:
        sql = """
            SELECT
                u.id AS user_id,
                u.email,
                u.username,
                p.full_name,
                p.bio,
                p.location,
                p.preferred_language,
                p.learning_history
            FROM users u
            LEFT JOIN profiles p
                ON p.user_id = u.id
               AND p.is_active = 'Y'
            WHERE u.id = :user_id
              AND u.is_active = 'Y'
            LIMIT 1
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql), {"user_id": user_id})
            row = result.mappings().first()
            if row is None:
                return None
            payload = dict(row)
            payload["user_id"] = _normalize_uuidish(payload.get("user_id"))
            payload["learning_history"] = _normalize_jsonish(payload.get("learning_history"), {})
            payload["skill_profile"] = await self.compute_skill_profile(user_id)
            return payload

    async def fetch_user_ratings(self, user_id: str) -> list[dict[str, Any]]:
        """Fetch user's course ratings. score >= 4 indicates preference."""
        sql = """
            SELECT
                r.course_id,
                r.rating AS score,
                c.title,
                c.level,
                COALESCE(
                    json_agg(DISTINCT s.name) FILTER (WHERE s.name IS NOT NULL),
                    '[]'::json
                ) AS skills
            FROM ratings r
            JOIN courses c ON c.id = r.course_id AND c.is_active = 'Y'
            LEFT JOIN course_skills cs ON cs.course_id = c.id
            LEFT JOIN skills s ON s.id = cs.skill_id
            WHERE r.user_id = :user_id
              AND r.is_active = 'Y'
            GROUP BY r.course_id, r.rating, c.title, c.level
            ORDER BY r.rating DESC
        """
        try:
            async with get_db_session() as session:
                result = await session.execute(text(sql), {"user_id": user_id})
                items = []
                for row in result.mappings().all():
                    payload = dict(row)
                    payload["course_id"] = _normalize_uuidish(payload.get("course_id"))
                    payload["score"] = int(payload.get("score") or 0)
                    payload["skills"] = _normalize_jsonish(payload.get("skills"), [])
                    items.append(payload)
                return items
        except Exception:
            return []

    async def fetch_course_prerequisites(self, course_id: str) -> list[str]:
        """Fetch prerequisite course IDs for a given course."""
        sql = """
            SELECT prerequisite_id
            FROM course_prerequisites
            WHERE course_id = :course_id
        """
        try:
            async with get_db_session() as session:
                result = await session.execute(text(sql), {"course_id": course_id})
                return [str(row["prerequisite_id"]) for row in result.mappings().all()]
        except Exception:
            return []

    async def compute_skill_profile(self, user_id: str) -> dict[str, float]:
        """Compute skill proficiency from completed courses and ratings.

        Logic:
        - completed course with rating >= 4 → skill = 0.9
        - completed course without rating or rating < 4 → skill = 0.7
        - in-progress course → skill = 0.3 * progress
        """
        course_history = await self.fetch_user_course_history(user_id)
        ratings_list = await self.fetch_user_ratings(user_id)
        ratings_map = {r["course_id"]: r["score"] for r in ratings_list}
        skills: dict[str, float] = {}
        for item in course_history:
            course_id = item.get("course_id", "")
            status = item.get("status", "")
            progress = float(item.get("progress", 0))
            course_skills = item.get("skills", [])
            rating = ratings_map.get(course_id, 0)
            for skill_name in course_skills:
                if not skill_name:
                    continue
                if status == "COMPLETED":
                    level = 0.9 if rating >= 4 else 0.7
                elif status == "IN_PROGRESS":
                    level = round(0.3 * progress, 2)
                else:
                    level = 0.1
                skills[skill_name] = max(skills.get(skill_name, 0), level)
        return skills

    async def fetch_user_course_history(self, user_id: str) -> list[dict[str, Any]]:
        sql = """
            SELECT
                e.id AS enrollment_id,
                e.course_id,
                e.status,
                c.title,
                c.description,
                c.level,
                c.language,
                c.thumbnail,
                COALESCE(AVG(p.completion), 0.0) AS progress,
                COALESCE(
                    json_agg(DISTINCT s.name) FILTER (WHERE s.name IS NOT NULL),
                    '[]'::json
                ) AS skills
            FROM enrollments e
            JOIN courses c
                ON c.id = e.course_id
               AND c.is_active = 'Y'
            LEFT JOIN chapters ch
                ON ch.course_id = c.id
            LEFT JOIN lessons l
                ON l.chapter_id = ch.id
               AND l.is_active = 'Y'
            LEFT JOIN progress p
                ON p.lesson_id = l.id
               AND p.user_id = e.user_id
               AND p.is_active = 'Y'
            LEFT JOIN course_skills cs
                ON cs.course_id = c.id
            LEFT JOIN skills s
                ON s.id = cs.skill_id
            WHERE e.user_id = :user_id
              AND e.is_active = 'Y'
            GROUP BY e.id, e.course_id, e.status, c.id
            ORDER BY MAX(e.updated) DESC NULLS LAST, MAX(e.created) DESC NULLS LAST
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql), {"user_id": user_id})
            items = []
            for row in result.mappings().all():
                payload = dict(row)
                payload["enrollment_id"] = _normalize_uuidish(payload.get("enrollment_id"))
                payload["course_id"] = _normalize_uuidish(payload.get("course_id"))
                payload["progress"] = float(payload.get("progress") or 0.0)
                payload["skills"] = _normalize_jsonish(payload.get("skills"), [])
                items.append(payload)
            return items

    async def fetch_user_learning_paths(self, user_id: str) -> list[dict[str, Any]]:
        sql = """
            SELECT
                lp.id AS path_id,
                lp.title,
                lp.description,
                COALESCE(MAX(pp.completion), 0.0) AS completion,
                COALESCE(
                    json_agg(
                        DISTINCT jsonb_build_object(
                            'course_id', lpc.course_id,
                            'order', lpc."order",
                            'position_x', lpc.position_x,
                            'position_y', lpc.position_y,
                            'is_optional', lpc.is_optional
                        )
                    ) FILTER (WHERE lpc.course_id IS NOT NULL),
                    '[]'::json
                ) AS courses
            FROM learning_paths lp
            LEFT JOIN path_progress pp
                ON pp.path_id = lp.id
               AND pp.user_id = :user_id
               AND pp.is_active = 'Y'
            LEFT JOIN learning_path_courses lpc
                ON lpc.path_id = lp.id
            WHERE lp.is_active = 'Y'
              AND pp.user_id = :user_id
            GROUP BY lp.id, lp.title, lp.description
            ORDER BY COALESCE(MAX(pp.updated), lp.updated) DESC NULLS LAST, lp.updated DESC
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql), {"user_id": user_id})
            items = []
            for row in result.mappings().all():
                payload = dict(row)
                payload["path_id"] = _normalize_uuidish(payload.get("path_id"))
                courses = _normalize_jsonish(payload.get("courses"), [])
                courses = sorted(
                    (
                        {
                            **course,
                            "course_id": _normalize_uuidish(course.get("course_id")),
                            "order": int(course.get("order") or 0),
                        }
                        for course in courses
                    ),
                    key=lambda item: item["order"],
                )
                payload["completion"] = float(payload.get("completion") or 0.0)
                payload["courses"] = courses
                items.append(payload)
            return items

    async def fetch_user_profiles_for_indexing(self) -> list[dict[str, Any]]:
        profiles = {
            item["user_id"]: item
            for item in await self._fetch_profiles_snapshot()
        }
        histories = await self._fetch_all_enrollment_histories()

        for user_id, courses in histories.items():
            profile = profiles.setdefault(
                user_id,
                {
                    "user_id": user_id,
                    "email": None,
                    "username": None,
                    "full_name": None,
                    "bio": None,
                    "location": None,
                    "preferred_language": None,
                    "learning_history": {},
                },
            )
            profile["course_history"] = courses

        for profile in profiles.values():
            profile.setdefault("course_history", [])
        return list(profiles.values())

    async def _fetch_profiles_snapshot(self) -> list[dict[str, Any]]:
        sql = """
            SELECT
                u.id AS user_id,
                u.email,
                u.username,
                p.full_name,
                p.bio,
                p.location,
                p.preferred_language,
                p.learning_history
            FROM users u
            LEFT JOIN profiles p
                ON p.user_id = u.id
               AND p.is_active = 'Y'
            WHERE u.is_active = 'Y'
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql))
            items = []
            for row in result.mappings().all():
                payload = dict(row)
                payload["user_id"] = _normalize_uuidish(payload.get("user_id"))
                payload["learning_history"] = _normalize_jsonish(payload.get("learning_history"), {})
                items.append(payload)
            return items

    async def _fetch_all_enrollment_histories(self) -> dict[str, list[dict[str, Any]]]:
        sql = """
            SELECT
                e.user_id,
                e.course_id,
                e.status,
                c.title,
                c.level,
                c.language,
                COALESCE(AVG(p.completion), 0.0) AS progress,
                COALESCE(
                    json_agg(DISTINCT s.name) FILTER (WHERE s.name IS NOT NULL),
                    '[]'::json
                ) AS skills
            FROM enrollments e
            JOIN courses c
                ON c.id = e.course_id
               AND c.is_active = 'Y'
            LEFT JOIN chapters ch
                ON ch.course_id = c.id
            LEFT JOIN lessons l
                ON l.chapter_id = ch.id
               AND l.is_active = 'Y'
            LEFT JOIN progress p
                ON p.lesson_id = l.id
               AND p.user_id = e.user_id
               AND p.is_active = 'Y'
            LEFT JOIN course_skills cs
                ON cs.course_id = c.id
            LEFT JOIN skills s
                ON s.id = cs.skill_id
            WHERE e.is_active = 'Y'
            GROUP BY e.user_id, e.course_id, e.status, c.id
            ORDER BY MAX(e.updated) DESC NULLS LAST, MAX(e.created) DESC NULLS LAST
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql))
            grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
            for row in result.mappings().all():
                payload = dict(row)
                user_id = _normalize_uuidish(payload.get("user_id"))
                if user_id is None:
                    continue
                grouped[user_id].append(
                    {
                        "course_id": _normalize_uuidish(payload.get("course_id")),
                        "status": payload.get("status"),
                        "title": payload.get("title"),
                        "level": payload.get("level"),
                        "language": payload.get("language"),
                        "progress": float(payload.get("progress") or 0.0),
                        "skills": _normalize_jsonish(payload.get("skills"), []),
                    }
                )
            return grouped

    @staticmethod
    def build_course_search_document(course: dict[str, Any]) -> str:
        parts: list[str] = [
            str(course.get("title") or ""),
            str(course.get("description") or ""),
            " ".join(course.get("objectives") or []),
            " ".join(course.get("requirements") or []),
            " ".join(course.get("tags") or []),
            " ".join(course.get("skills") or []),
            str(course.get("level") or ""),
            str(course.get("language") or ""),
        ]
        return "\n".join(part for part in parts if part).strip()

    @staticmethod
    def build_lesson_search_document(lesson: dict[str, Any]) -> str:
        parts: list[str] = [
            str(lesson.get("course_title") or ""),
            str(lesson.get("title") or ""),
            str(lesson.get("description") or ""),
            str(lesson.get("content") or ""),
            " ".join(lesson.get("workspace_languages") or []),
            " ".join(lesson.get("document_urls") or []),
        ]
        return "\n".join(part for part in parts if part).strip()

    @staticmethod
    def summarize_user_profile(profile: dict[str, Any], course_history: list[dict[str, Any]]) -> str:
        completed = [item["title"] for item in course_history if item.get("status") == "COMPLETED"]
        in_progress = [item["title"] for item in course_history if item.get("status") == "IN_PROGRESS"]
        skills = sorted({skill for item in course_history for skill in item.get("skills", [])})
        history = profile.get("learning_history") or {}
        if isinstance(history, dict):
            history_text = json.dumps(history, ensure_ascii=False)
        else:
            history_text = str(history)

        parts = [
            f"user={profile.get('user_id')}",
            f"name={profile.get('full_name') or profile.get('username') or ''}",
            f"bio={profile.get('bio') or ''}",
            f"preferred_language={profile.get('preferred_language') or ''}",
            f"skills={', '.join(skills)}",
            f"completed_courses={', '.join(completed)}",
            f"in_progress_courses={', '.join(in_progress)}",
            f"learning_history={history_text}",
        ]
        return "\n".join(part for part in parts if part.strip()).strip()

    async def fetch_collaborative_candidates(
        self, user_id: str, limit: int = 20
    ) -> list[dict[str, Any]]:
        """Return candidate courses for collaborative filtering.

        Uses a 4-CTE query to:
        1. Find courses the target user is enrolled in.
        2. Aggregate their tag/skill context.
        3. Find co-learners (users sharing at least one enrolled course).
        4. Rank courses the co-learners are in that the user has not seen yet,
           by co-enrollment count.
        """
        sql = """
            WITH user_courses AS (
                SELECT e.course_id
                FROM enrollments e
                WHERE e.user_id = :user_id
                  AND e.is_active = 'Y'
                  AND e.status NOT IN ('DROPPED')
            ),
            user_context AS (
                SELECT
                    COALESCE(
                        json_agg(DISTINCT t.name) FILTER (WHERE t.name IS NOT NULL),
                        '[]'::json
                    ) AS user_tags,
                    COALESCE(
                        json_agg(DISTINCT s.name) FILTER (WHERE s.name IS NOT NULL),
                        '[]'::json
                    ) AS user_skills
                FROM user_courses uc
                LEFT JOIN course_tags ct   ON ct.course_id  = uc.course_id
                LEFT JOIN tags t           ON t.id = ct.tag_id AND t.is_active = 'Y'
                LEFT JOIN course_skills cs ON cs.course_id = uc.course_id
                LEFT JOIN skills s         ON s.id = cs.skill_id AND s.is_active = 'Y'
            ),
            co_learners AS (
                SELECT DISTINCT e.user_id
                FROM enrollments e
                WHERE e.course_id IN (SELECT course_id FROM user_courses)
                  AND e.user_id != :user_id
                  AND e.is_active = 'Y'
                  AND e.status NOT IN ('DROPPED')
            ),
            co_enrolled AS (
                SELECT e.course_id, COUNT(DISTINCT e.user_id) AS co_count
                FROM enrollments e
                WHERE e.user_id IN (SELECT user_id FROM co_learners)
                  AND e.course_id NOT IN (SELECT course_id FROM user_courses)
                  AND e.is_active = 'Y'
                  AND e.status NOT IN ('DROPPED')
                GROUP BY e.course_id
            )
            SELECT
                c.id,
                c.title,
                c.description,
                c.level,
                c.language,
                c.thumbnail,
                c.instructor_id,
                ce.co_count,
                COALESCE(
                    json_agg(DISTINCT t.name)  FILTER (WHERE t.name  IS NOT NULL),
                    '[]'::json
                ) AS tags,
                COALESCE(
                    json_agg(DISTINCT s.name)  FILTER (WHERE s.name  IS NOT NULL),
                    '[]'::json
                ) AS skills,
                (SELECT user_tags   FROM user_context) AS user_tags,
                (SELECT user_skills FROM user_context) AS user_skills
            FROM co_enrolled ce
            JOIN courses c ON c.id = ce.course_id
                AND c.is_active = 'Y'
                AND c.status = 'PUBLISHED'
            LEFT JOIN course_tags   ct ON ct.course_id = c.id
            LEFT JOIN tags          t  ON t.id  = ct.tag_id  AND t.is_active = 'Y'
            LEFT JOIN course_skills cs ON cs.course_id = c.id
            LEFT JOIN skills        s  ON s.id  = cs.skill_id AND s.is_active = 'Y'
            GROUP BY c.id, c.title, c.description, c.level, c.language, c.thumbnail,
                     c.instructor_id, ce.co_count
            ORDER BY ce.co_count DESC
            LIMIT :limit
        """
        async with get_db_session() as session:
            result = await session.execute(
                text(sql), {"user_id": user_id, "limit": limit}
            )
            items: list[dict[str, Any]] = []
            for row in result.mappings().all():
                payload = dict(row)
                payload["id"] = _normalize_uuidish(payload.get("id"))
                payload["instructor_id"] = _normalize_uuidish(payload.get("instructor_id"))
                payload["co_count"] = int(payload.get("co_count") or 0)
                payload["tags"] = _normalize_jsonish(payload.get("tags"), [])
                payload["skills"] = _normalize_jsonish(payload.get("skills"), [])
                payload["user_tags"] = _normalize_jsonish(payload.get("user_tags"), [])
                payload["user_skills"] = _normalize_jsonish(payload.get("user_skills"), [])
                items.append(payload)
            return items

    @staticmethod
    def _normalize_course_row(row: dict[str, Any]) -> dict[str, Any]:
        row["id"] = _normalize_uuidish(row.get("id"))
        row["instructor_id"] = _normalize_uuidish(row.get("instructor_id"))
        row["objectives"] = _normalize_jsonish(row.get("objectives"), [])
        row["requirements"] = _normalize_jsonish(row.get("requirements"), [])
        row["tags"] = _normalize_jsonish(row.get("tags"), [])
        row["skills"] = _normalize_jsonish(row.get("skills"), [])
        row["enrollment_count"] = int(row.get("enrollment_count") or 0)
        return row

    @staticmethod
    def _normalize_lesson_row(row: dict[str, Any]) -> dict[str, Any]:
        row["id"] = _normalize_uuidish(row.get("id"))
        row["course_id"] = _normalize_uuidish(row.get("course_id"))
        row["chapter_id"] = _normalize_uuidish(row.get("chapter_id"))
        row["document_urls"] = _normalize_jsonish(row.get("document_urls"), [])
        row["workspace_languages"] = _normalize_jsonish(row.get("workspace_languages"), [])
        row["workspace_template"] = _normalize_jsonish(row.get("workspace_template"), {})
        return row


catalog_service = CatalogService()
